package chat.simplex.matrix

import chat.simplex.matrix.adapter.MatrixItemKind
import chat.simplex.matrix.adapter.MatrixMessageDto
import chat.simplex.matrix.adapter.MatrixRoomSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.matrix.rustcomponents.sdk.EventOrTransactionId
import org.matrix.rustcomponents.sdk.MsgLikeContent
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.Room
import org.matrix.rustcomponents.sdk.RoomList
import org.matrix.rustcomponents.sdk.RoomListEntriesListener
import org.matrix.rustcomponents.sdk.RoomListEntriesUpdate
import org.matrix.rustcomponents.sdk.TaskHandle
import org.matrix.rustcomponents.sdk.Timeline
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem
import org.matrix.rustcomponents.sdk.TimelineItemContent
import org.matrix.rustcomponents.sdk.TimelineListener

/**
 * Engine layer (#134): observes the room list (DMs via `Client.getDmRooms`,
 * same as the verified P0 spike) and attaches one timeline listener per
 * opened room, converting SDK TimelineItems into adapter DTOs. This is the
 * ONLY place where `org.matrix.rustcomponents.sdk` types are turned into
 * `chat.simplex.matrix.adapter` types (DTO boundary rule, SPECS.md §3).
 */
class MatrixRoomsObserver(
    private val engine: MatrixEngine,
    private val onLog: (String) -> Unit = {},
) {

    /** Room summaries by roomId - the adapter's Direct/Group context. */
    private val roomSummaries = linkedMapOf<String, MatrixRoomSummary>()

    private val openTimelines = linkedMapOf<String, Timeline>()
    private val timelineHandles = linkedMapOf<String, TaskHandle>()

    private var allRoomsList: RoomList? = null
    private var allRoomsStream: TaskHandle? = null

    /**
     * Subscribes to the room list service's full entry stream. This is the
     * reliable discovery path: `Client.getDmRooms` stayed empty on the local
     * Synapse bench even with correct m.direct account data (see #134), while
     * the room list service tracks every room the account knows (the spike
     * opened rooms by id through it successfully). Every direct room in an
     * Append update is converted and forwarded; non-direct rooms are skipped
     * until group support lands (#148).
     */
    suspend fun subscribeAllRooms(scope: kotlinx.coroutines.CoroutineScope, onRoom: (MatrixRoomSummary) -> Unit) {
        val svc = engine.syncService ?: error("start sync first")
        val list = svc.roomListService().allRooms()
        allRoomsList = list
        val result = list.entriesWithDynamicAdapters(50u, object : RoomListEntriesListener {
            override fun onUpdate(updates: List<RoomListEntriesUpdate>) {
                updates.forEach { update ->
                    if (update is RoomListEntriesUpdate.Append) {
                        update.values.forEach { room ->
                            // isDirect() is a suspend call: the FFI listener thread
                            // must not block, so conversion runs in [scope].
                            scope.launch {
                                try {
                                    val direct = room.isDirect()
                                    if (!direct) {
                                        onLog("room ${room.id()} skipped (not direct, groups = #148)")
                                        return@launch
                                    }
                                    roomSummary(room, isDm = true)?.let { summary ->
                                        roomSummaries[summary.roomId] = summary
                                        onRoom(summary)
                                    }
                                } catch (e: Exception) {
                                    onLog("room ${room.id()} convert failed: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        })
        allRoomsStream = result.entriesStream()
        // The dynamic adapter ships with NO filter: without an explicit one the
        // stream stays silent even though the room list is populated behind it.
        runCatching {
            result.controller().setFilter(org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.All(emptyList()))
            result.controller().addOnePage()
        }.onFailure { onLog("filter setup failed: ${it.message}") }
        onLog("subscribed to all-rooms stream")
    }

    /**
     * Waits for the sliding-sync room list to populate (retry window:
     * 10 x 2 s, matching the P0 spike), then returns DM room summaries.
     * Safe to call again later; new rooms are appended.
     */
    suspend fun awaitDmRooms(): List<MatrixRoomSummary> {
        val userId = try {
            engine.client.userId()
        } catch (e: Exception) {
            onLog("userId failed: ${e.message}")
            return emptyList()
        }
        var rooms: List<Room> = emptyList()
        var lastError: Exception? = null
        repeat(15) { attempt ->
            if (rooms.isEmpty()) {
                try {
                    rooms = engine.client.getDmRooms(userId)
                } catch (e: Exception) {
                    lastError = e
                    if (attempt == 0) onLog("waiting for room list (attempt ${attempt + 1})")
                }
                // Delay on BOTH failure paths: an empty (non-throwing) result is
                // the common case while sliding sync is still populating, and
                // without this delay the loop burned all attempts instantly.
                if (rooms.isEmpty()) delay(2000)
            }
        }
        if (rooms.isEmpty()) {
            onLog("no rooms in list yet: ${lastError?.message}")
            return emptyList()
        }
        val summaries = rooms.mapNotNull { room -> roomSummary(room, isDm = true) }
        summaries.forEach { roomSummaries[it.roomId] = it }
        return summaries
    }

    fun summaryOf(roomId: String): MatrixRoomSummary? = roomSummaries[roomId]

    fun isOpen(roomId: String): Boolean = openTimelines.containsKey(roomId)

    /**
     * Opens a room timeline (with the same retry window the spike used) and
     * streams live diffs as DTOs to [onMessage]. Returns false when the room
     * never appears in the list.
     */
    suspend fun openRoom(roomId: String, onMessage: (MatrixMessageDto) -> Unit): Boolean {
        if (openTimelines.containsKey(roomId)) return true
        val svc = engine.syncService ?: error("start sync first")
        var room: Room? = null
        var lastError: Exception? = null
        repeat(10) { attempt ->
            if (room == null) {
                try {
                    room = svc.roomListService().room(roomId)
                } catch (e: Exception) {
                    lastError = e
                    if (attempt == 0) onLog("waiting for room list (attempt ${attempt + 1})")
                    delay(2000)
                }
            }
        }
        val r = room ?: run {
            onLog("room not in list yet: ${lastError?.message}")
            return false
        }
        roomSummaries.getOrPut(roomId) { roomSummary(r, isDm = true) ?: MatrixRoomSummary(roomId, roomId, isDm = true, isEncrypted = false) }
        val timeline = r.timeline()
        val handle = timeline.addListener(object : TimelineListener {
            override fun onUpdate(diffs: List<TimelineDiff>) {
                diffs.forEach { diff -> renderDiff(roomId, diff, onMessage) }
            }
        })
        // Subscribe the room in the sliding-sync session: without this the
        // server never pushes the room's updates (the sync pos stalls and the
        // timeline listener stays silent after the initial sync window).
        try {
            svc.roomListService().setRoomSubscriptions(listOf(roomId))
        } catch (e: Exception) {
            onLog("room subscription failed: ${e.message}")
        }
        openTimelines[roomId] = timeline
        timelineHandles[roomId] = handle
        onLog("timeline open: ${r.displayName()} (id=${r.id()})")
        return true
    }

    fun closeRoom(roomId: String) {
        timelineHandles.remove(roomId)?.close()
        openTimelines.remove(roomId)?.let { runCatching { it.close() } }
    }

    fun closeAll() {
        timelineHandles.values.forEach { it.close() }
        timelineHandles.clear()
        openTimelines.values.forEach { runCatching { it.close() } }
        openTimelines.clear()
        allRoomsStream?.close()
        allRoomsStream = null
        runCatching { allRoomsList?.close() }
        allRoomsList = null
    }

    private suspend fun roomSummary(room: Room, isDm: Boolean): MatrixRoomSummary? = try {
        MatrixRoomSummary(
            roomId = room.id(),
            displayName = room.displayName() ?: room.id(),
            isDm = isDm,
            isEncrypted = room.isEncrypted(),
        )
    } catch (e: Exception) {
        onLog("room summary failed for ${room.id()}: ${e.message}")
        null
    }

    private fun renderDiff(roomId: String, diff: TimelineDiff, onMessage: (MatrixMessageDto) -> Unit) {
        when (diff) {
            is TimelineDiff.Append -> diff.values.forEach { emit(roomId, it, onMessage) }
            is TimelineDiff.PushBack -> emit(roomId, diff.value, onMessage)
            is TimelineDiff.PushFront -> emit(roomId, diff.value, onMessage)
            is TimelineDiff.Insert -> emit(roomId, diff.value, onMessage)
            is TimelineDiff.Set -> emit(roomId, diff.value, onMessage)
            is TimelineDiff.Reset -> diff.values.forEach { emit(roomId, it, onMessage) }
            // P1 is live-append only (SPECS.md §4); removals are no-ops.
            is TimelineDiff.Remove, is TimelineDiff.Clear, is TimelineDiff.Truncate,
            is TimelineDiff.PopBack, is TimelineDiff.PopFront -> {}
        }
    }

    /** Generalization of the spike's itemText extraction: SDK item -> DTO, null = not a message. */
    private fun emit(roomId: String, item: TimelineItem, onMessage: (MatrixMessageDto) -> Unit) {
        val dto = toItemDto(roomId, item) ?: return
        onMessage(dto)
    }

    private fun toItemDto(roomId: String, item: TimelineItem): MatrixMessageDto? = try {
        val ev = item.asEvent() ?: return null
        val eventId = when (val id = ev.eventOrTransactionId) {
            is EventOrTransactionId.EventId -> id.eventId
            is EventOrTransactionId.TransactionId -> id.transactionId
        }
        // P1: sender display name falls back to the MXID; the profile lookup
        // (ProfileDetails.Ready) is P2 polish.
        val senderName = ev.sender
        when (val content = ev.content) {
            is TimelineItemContent.MsgLike -> {
                val ml = content.content
                MatrixMessageDto(
                    roomId = roomId,
                    eventId = eventId,
                    senderId = ev.sender,
                    senderName = senderName,
                    body = bodyOf(ml),
                    timestampMs = ev.timestamp.toLong(),
                    isOwn = ev.isOwn,
                    kind = kindOf(ml),
                    isEdited = (ml as? MsgLikeKind.Message)?.content?.isEdited == true,
                )
            }
            // Membership/state/etc. events are not chat messages; ignore.
            else -> null
        }
    } catch (e: Exception) {
        onLog("item extract failed: ${e.message}")
        null
    }

    private fun bodyOf(ml: MsgLikeContent): String = when (val kind = ml.kind) {
        is MsgLikeKind.Message -> kind.content.body
        is MsgLikeKind.UnableToDecrypt -> ""
        is MsgLikeKind.Redacted -> ""
        else -> ""
    }

    private fun kindOf(ml: MsgLikeContent): MatrixItemKind = when (val kind = ml.kind) {
        is MsgLikeKind.Message -> when (val mt = kind.content.msgType) {
            is org.matrix.rustcomponents.sdk.MessageType.Text,
            is org.matrix.rustcomponents.sdk.MessageType.Emote,
            is org.matrix.rustcomponents.sdk.MessageType.Notice -> MatrixItemKind.TEXT
            is org.matrix.rustcomponents.sdk.MessageType.Image -> MatrixItemKind.IMAGE
            is org.matrix.rustcomponents.sdk.MessageType.Video -> MatrixItemKind.VIDEO
            is org.matrix.rustcomponents.sdk.MessageType.Audio -> MatrixItemKind.AUDIO
            else -> MatrixItemKind.OTHER
        }
        is MsgLikeKind.UnableToDecrypt -> MatrixItemKind.UNABLE_TO_DECRYPT
        is MsgLikeKind.Redacted -> MatrixItemKind.REDACTED
        else -> MatrixItemKind.OTHER
    }
}
