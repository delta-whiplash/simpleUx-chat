package chat.simplex.matrix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.SyncService
import org.matrix.rustcomponents.sdk.SyncServiceState
import org.matrix.rustcomponents.sdk.SyncServiceStateObserver
import org.matrix.rustcomponents.sdk.TaskHandle

/**
 * Engine layer (#134): owns the rust-sdk client + SyncService lifecycle and
 * the engine coroutine scope. Sync start/stop policy in SPECS.md §5:
 * idempotent start, best-effort stop, one scope, ordered shutdown.
 */
class MatrixEngine(val client: Client) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var syncService: SyncService? = null
        private set

    var onLog: (String) -> Unit = {}

    private var stateHandle: TaskHandle? = null

    val loggedIn: Boolean get() = true

    suspend fun startSync() {
        if (syncService != null) {
            onLog("sync already running")
            return
        }
        val svc = client.syncService().withOfflineMode().finish()
        stateHandle = svc.state(object : SyncServiceStateObserver {
            override fun onUpdate(state: SyncServiceState) {
                onLog("sync state: ${state::class.simpleName}")
            }
        })
        svc.start()
        syncService = svc
        onLog("sync service started")
    }

    suspend fun stopSync() {
        val svc = syncService ?: return
        runCatching { svc.stop() }.onFailure { onLog("sync stop failed: ${it.message}") }
        runCatching { svc.close() }
        stateHandle?.close()
        stateHandle = null
        syncService = null
        onLog("sync service stopped")
    }

    /** Full ordered shutdown (timelines and listeners are closed by the observer layer). */
    fun shutdown() {
        stateHandle?.close()
        stateHandle = null
        runCatching { syncService?.close() }
        syncService = null
        scope.cancel()
        runCatching { client.close() }
    }
}
