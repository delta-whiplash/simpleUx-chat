package chat.simplex.matrix

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.Room
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.SlidingSyncVersion
import org.matrix.rustcomponents.sdk.SyncService
import org.matrix.rustcomponents.sdk.SyncServiceState
import org.matrix.rustcomponents.sdk.SyncServiceStateObserver
import org.matrix.rustcomponents.sdk.TaskHandle
import org.matrix.rustcomponents.sdk.Timeline
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem
import org.matrix.rustcomponents.sdk.TimelineItemContent
import org.matrix.rustcomponents.sdk.TimelineListener
import java.io.File
import java.util.Properties

/**
 * Spike engine for the Matrix co-protocol (#128, P0 = #129..#132).
 *
 * Holds the rust-sdk client, sync service and one open timeline, and keeps a
 * plain text log that MatrixDebugActivity renders. Deliberately primitive:
 * no bridge into ChatModel yet (that is P1, #134-#140), no UX polish. It
 * exists to prove, with measurements, that the engine embeds cleanly before
 * any integration work starts.
 */
object MatrixSpike {

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var client: Client? = null
        private set
    var syncService: SyncService? = null
        private set

    private var openTimeline: Timeline? = null
    private var timelineHandle: TaskHandle? = null
    private var stateHandle: TaskHandle? = null

    private val logBuf = ArrayDeque<String>(500)
    var onLog: (() -> Unit)? = null

    fun log(line: String) {
        synchronized(logBuf) {
            if (logBuf.size == 500) logBuf.removeFirst()
            logBuf.addLast(line)
        }
        onLog?.invoke()
    }

    fun logText(): String = synchronized(logBuf) { logBuf.joinToString("\n") }

    private fun dataDir(ctx: Context): File =
        File(ctx.filesDir, "matrix-spike/data").apply { mkdirs() }

    private fun cacheDir(ctx: Context): File =
        File(ctx.cacheDir, "matrix-spike/cache").apply { mkdirs() }

    private fun sessionFile(ctx: Context): File =
        File(ctx.filesDir, "matrix-spike/session.properties")

    private suspend fun newClient(ctx: Context, homeserverUrl: String): Client =
        ClientBuilder()
            .homeserverUrl(homeserverUrl)
            .sessionPaths(dataDir(ctx).absolutePath, cacheDir(ctx).absolutePath)
            .build()

    val loggedIn: Boolean get() = client != null

    suspend fun loginPassword(ctx: Context, homeserverUrl: String, username: String, password: String) {
        val c = newClient(ctx, homeserverUrl)
        try {
            c.login(username, password, "SimpleUX Spike", null)
        } catch (e: Exception) {
            c.close()
            throw e
        }
        client = c
        saveSession(ctx, c)
        log("logged in as ${c.userId()} (device ${c.deviceId()})")
    }

    /** Rebuild the client from the persisted session; true if a session was restored. */
    suspend fun restoreSession(ctx: Context): Boolean {
        val f = sessionFile(ctx)
        if (!f.exists()) return false
        val props = Properties().apply { f.inputStream().use { load(it) } }
        val session = SpikeSession.fromProperties(props) ?: run {
            log("stored session is incomplete, login required")
            return false
        }
        val c = newClient(ctx, session.homeserverUrl)
        try {
            c.restoreSession(session.toSdkSession())
        } catch (e: Exception) {
            c.close()
            log("restoreSession failed: ${e.message}")
            return false
        }
        client = c
        log("session restored for ${c.userId()}")
        return true
    }

    suspend fun logout(ctx: Context) {
        stopSync()
        openTimeline?.close()
        openTimeline = null
        client?.let { c ->
            runCatching { c.logout() }.onFailure { log("logout call failed: ${it.message}") }
            runCatching { c.close() }
        }
        client = null
        sessionFile(ctx).delete()
        File(ctx.filesDir, "matrix-spike/data").deleteRecursively()
        File(ctx.cacheDir, "matrix-spike/cache").deleteRecursively()
        log("logged out, local data wiped")
    }

    suspend fun startSync() {
        val c = client ?: error("not logged in")
        if (syncService != null) { log("sync already running"); return }
        val svc = c.syncService().withOfflineMode().finish()
        stateHandle = svc.state(object : SyncServiceStateObserver {
            override fun onUpdate(state: SyncServiceState) {
                log("sync state: ${state::class.simpleName}")
            }
        })
        svc.start()
        syncService = svc
        log("sync service started")
    }

    suspend fun stopSync() {
        val svc = syncService ?: return
        runCatching { svc.stop() }.onFailure { log("sync stop failed: ${it.message}") }
        runCatching { svc.close() }
        stateHandle?.close()
        stateHandle = null
        syncService = null
        log("sync service stopped")
    }

    /** Lists DM rooms of the own user into the log; returns their room ids. */
    suspend fun listDms(): List<String> {
        val c = client ?: error("not logged in")
        val userId = try { c.userId() } catch (e: Exception) { error("userId failed: ${e.message}") }
        val dms = c.getDmRooms(userId)
        if (dms.isEmpty()) log("no DM rooms yet (sync may still be catching up)")
        dms.forEach { room ->
            try {
                val enc = room.isEncrypted()
                log("DM ${room.id()}  name=${room.displayName()}  encrypted=$enc")
            } catch (e: Exception) {
                log("DM ${room.id()}  <error ${e.message}>")
            }
        }
        return dms.map { it.id() }
    }

    /** Opens a room timeline and streams decrypted items into the log (#131). */
    suspend fun openRoom(roomId: String) {
        val svc = syncService ?: error("start sync first")
        openTimeline?.let { runCatching { it.close() } }
        openTimeline = null
        timelineHandle?.close()

        // The sliding-sync room list needs a moment to populate after start().
        var room: Room? = null
        var lastError: Exception? = null
        repeat(10) { attempt ->
            if (room == null) {
                try {
                    room = svc.roomListService().room(roomId)
                } catch (e: Exception) {
                    lastError = e
                    if (attempt == 0) log("waiting for room list (attempt ${attempt + 1})")
                    kotlinx.coroutines.delay(2000)
                }
            }
        }
        val r = room ?: error("room not in list yet: ${lastError?.message}")
        val enc = try { r.isEncrypted() } catch (e: Exception) { false }
        val timeline = r.timeline()
        timelineHandle = timeline.addListener(object : TimelineListener {
            override fun onUpdate(diffs: List<TimelineDiff>) {
                diffs.forEach { diff -> renderDiff(diff) }
            }
        })
        openTimeline = timeline
        log("timeline open: ${r.displayName()} (id=${r.id()}, encrypted=$enc)")
    }

    private fun renderDiff(diff: TimelineDiff) {
        when (diff) {
            is TimelineDiff.Append -> diff.values.forEach { renderItem(it, "append") }
            is TimelineDiff.PushBack -> renderItem(diff.value, "pushBack")
            is TimelineDiff.PushFront -> renderItem(diff.value, "pushFront")
            is TimelineDiff.Insert -> renderItem(diff.value, "insert")
            is TimelineDiff.Set -> renderItem(diff.value, "set")
            is TimelineDiff.Remove -> log("item removed at ${diff.index}")
            is TimelineDiff.Clear -> log("timeline cleared")
            is TimelineDiff.Reset -> diff.values.forEach { renderItem(it, "reset") }
            is TimelineDiff.Truncate -> log("truncated to ${diff.length}")
            is TimelineDiff.PopBack, is TimelineDiff.PopFront -> {}
        }
    }

    private fun renderItem(item: TimelineItem, origin: String) {
        val text = itemText(item) ?: return
        log("[$origin] $text")
    }

    /** Best-effort extraction: the goal is seeing decrypted plaintext in the log. */
    private fun itemText(item: TimelineItem): String? {
        return try {
            val ev = item.asEvent() ?: return null
        val who = if (ev.isOwn) "me" else ev.sender
        when (val content = ev.content) {
            is TimelineItemContent.MsgLike -> when (val kind = content.content.kind) {
                is MsgLikeKind.Message -> "<$who> ${kind.content.body}"
                is MsgLikeKind.UnableToDecrypt -> "<$who> ** UTD (unable to decrypt) **"
                is MsgLikeKind.Redacted -> "<$who> (message redacted)"
                else -> "<$who> (${kind::class.simpleName})"
            }
            is TimelineItemContent.RoomMembership -> "<$who> room membership"
            is TimelineItemContent.ProfileChange -> "<$who> profile change"
            is TimelineItemContent.State -> "<$who> room state"
            else -> null
        }
        } catch (e: Exception) {
            "<item extract failed: ${e.message}>"
        }
    }

    fun shutdown() {
        scope.cancel()
        runCatching { openTimeline?.close() }
        timelineHandle?.close()
        runCatching { syncService?.close() }
        stateHandle?.close()
        runCatching { client?.close() }
        client = null
        syncService = null
    }

    private fun saveSession(ctx: Context, c: Client) {
        val s = try { c.session() } catch (e: Exception) {
            log("session() unavailable, persistence skipped: ${e.message}")
            return
        }
        val props = Properties().apply {
            setProperty("userId", s.userId)
            setProperty("deviceId", s.deviceId)
            setProperty("accessToken", s.accessToken)
            setProperty("homeserverUrl", s.homeserverUrl)
            s.refreshToken?.let { setProperty("refreshToken", it) }
        }
        sessionFile(ctx).parentFile?.mkdirs()
        sessionFile(ctx).outputStream().use { props.store(it, "matrix spike session") }
    }
}

/** Minimal property-based session carrier for the spike (P1 will use the SDK store properly). */
private data class SpikeSession(
    val userId: String,
    val deviceId: String,
    val accessToken: String,
    val refreshToken: String?,
    val homeserverUrl: String,
) {
    fun toSdkSession(): Session = Session(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        deviceId = deviceId,
        homeserverUrl = homeserverUrl,
        oauthData = null,
        slidingSyncVersion = SlidingSyncVersion.NATIVE,
    )

    companion object {
        fun fromProperties(p: Properties): SpikeSession? {
            return SpikeSession(
                userId = p.getProperty("userId") ?: return null,
                deviceId = p.getProperty("deviceId") ?: return null,
                accessToken = p.getProperty("accessToken") ?: return null,
                refreshToken = p.getProperty("refreshToken"),
                homeserverUrl = p.getProperty("homeserverUrl") ?: return null,
            )
        }
    }
}
