package chat.simplex.matrix

import android.content.Context
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.ChatModel
import chat.simplex.matrix.adapter.MatrixAdapter
import chat.simplex.matrix.adapter.MatrixMessageDto
import chat.simplex.matrix.adapter.MatrixRoomSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridge layer (#136): the single entry point that wires the Matrix engine
 * into SimpleUX UI state. Ingestion happens ONLY via ChatModel.chatsContext
 * public methods, on the main dispatcher - the same threading contract as
 * the SimpleX receiver (`processReceivedMsg` wraps every chatsContext
 * mutation in `withContext(Dispatchers.Main)`).
 *
 * Nothing calls [start] from app code yet: wiring is the orchestrator's
 * integration step. MatrixDebugActivity uses the lower-level primitives.
 */
object MatrixBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var account: MatrixAccount? = null
    private var engine: MatrixEngine? = null
    private var observer: MatrixRoomsObserver? = null

    /** Room summaries by roomId, so timeline items can be scoped to their chat. */
    private val rooms = linkedMapOf<String, MatrixRoomSummary>()

    var onLog: (String) -> Unit = {}

    val started: Boolean get() = engine != null
    val running: Boolean get() = engine?.syncService != null

    /**
     * Restore the stored session (if any) and start ingestion: DM rooms are
     * pushed into the chat list (batched, one main-thread hop per chat),
     * then each opened room streams timeline items through
     * chatsContext.addChatItem. Returns false when no session file exists.
     */
    fun start(context: Context): Boolean {
        val acct = account ?: MatrixAccount(context.applicationContext).also { account = it }
        if (engine != null) return true
        if (!acct.hasSession()) {
            log("no stored matrix session, bridge idle")
            return false
        }
        scope.launch {
            val client = acct.restore { log(it) } ?: return@launch
            startEngine(client)
        }
        return true
    }

    /**
     * Login helper (used by the debug bench; the product login flow is a
     * later phase). Starts ingestion on success.
     */
    fun login(context: Context, homeserverUrl: String, username: String, password: String) {
        val acct = account ?: MatrixAccount(context.applicationContext).also { account = it }
        scope.launch {
            startEngine(acct.login(homeserverUrl, username, password) { log(it) })
        }
    }

    /** Open a room's timeline and stream its items into the open-conversation scope. */
    fun openRoom(roomId: String) {
        scope.launch {
            // start() is asynchronous: the observer appears only once the
            // engine has booted. Wait for it (bounded) instead of silently
            // dropping the open request - this races at every app start where
            // a stored Matrix session exists.
            var obs = observer
            var tries = 0
            while (obs == null && tries++ < 15) {
                kotlinx.coroutines.delay(2000)
                obs = observer
            }
            if (obs == null) {
                log("openRoom($roomId): bridge not running")
                return@launch
            }
            if (obs.openRoom(roomId) { dto -> ingestMessage(dto) }) {
                // Register + ingest the chat even when it was not discovered
                // via DM markers (rooms created without m.direct account data
                // never appear in getDmRooms; opening by id must still work).
                val summary = obs.summaryOf(roomId)
                when {
                    summary == null -> log("openRoom: no summary for $roomId")
                    rooms[roomId] != null -> {} // already registered
                    else -> {
                        registerRoom(summary)
                        log("ingested chat id=${MatrixAdapter.matrixChatId(summary.roomId)}")
                    }
                }
            }
        }
    }

    fun isOpen(roomId: String): Boolean = observer?.isOpen(roomId) == true

    /** Stop sync and release everything except the stored session (idempotent). */
    fun stop() {
        observer?.closeAll()
        observer = null
        rooms.clear()
        val eng = engine
        engine = null
        if (eng != null) {
            scope.launch {
                eng.stopSync()
                eng.shutdown()
            }
        }
    }

    /** Full logout + local wipe. */
    fun logout(context: Context) {
        val eng = engine
        stop()
        val acct = account ?: MatrixAccount(context.applicationContext).also { account = it }
        account = null
        scope.launch { acct.logout(eng?.client) { log(it) } }
    }

    /** Test/bench hook to detach the log. */
    fun shutdown() {
        stop()
        scope.cancel()
    }

    private suspend fun startEngine(client: org.matrix.rustcomponents.sdk.Client) {
        val eng = MatrixEngine(client).also { it.onLog = { log(it) } }
        engine = eng
        eng.startSync()
        val obs = MatrixRoomsObserver(eng) { log(it) }
        observer = obs
        // Best-effort immediate discovery, then the event-driven all-rooms
        // stream (authoritative on servers where it works). Rooms already
        // known from previous sessions are re-opened directly: the direct
        // room lookup is the one path proven to work everywhere (#134).
        for (summary in obs.awaitDmRooms()) {
            registerRoom(summary)
        }
        obs.subscribeAllRooms(scope) { summary -> registerRoom(summary) }
        val known = account?.knownRooms().orEmpty()
        log("re-opening ${known.size} known room(s)")
        known.forEach { openRoom(it) }
        log("bridge started: ${rooms.size} room(s) so far, all-rooms stream subscribed")
    }

    private fun registerRoom(summary: MatrixRoomSummary) {
        if (rooms[summary.roomId] != null) return
        rooms[summary.roomId] = summary
        account?.rememberRoom(summary.roomId)
        scope.launch { ingestChat(summary) }
    }

    // ---- ingestion (chatsContext public methods only, on Main) ----

    private suspend fun ingestChat(summary: MatrixRoomSummary, previewItem: ChatItem? = null) {
        val chat: Chat = MatrixAdapter.roomToChat(summary, previewItem)
        withContext(Dispatchers.Main) {
            if (!ChatModel.chatsContext.hasChat(null, chat.id)) {
                ChatModel.chatsContext.addChat(chat)
                log("chatsContext.addChat done: ${chat.id} (list size now ${ChatModel.chats.value.size})")
            } else {
                log("ingestChat: chat already present ${chat.id}")
            }
        }
    }

    private fun ingestMessage(dto: MatrixMessageDto) {
        val room = rooms[dto.roomId] ?: return
        val cInfo = MatrixAdapter.roomToChatInfo(room)
        val cItem = MatrixAdapter.messageToChatItem(dto, room) ?: return
        scope.launch {
            withContext(Dispatchers.Main) {
                ChatModel.chatsContext.addChatItem(null, cInfo, cItem)
            }
        }
    }

    private fun log(line: String) {
        // Always mirror to logcat: the hook runs without the bench screen
        // attached in real app starts, and bench runs span activity switches.
        android.util.Log.d("MatrixBridge", line)
        onLog(line)
    }
}
