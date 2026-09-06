package chat.simplex.matrix

import android.content.Context
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.SlidingSyncVersion
import java.io.File
import java.util.Properties

/**
 * Account layer (#134): owns the Matrix session lifecycle - password login,
 * persistence across restarts and restore. See SPECS.md §1 for the state
 * machine and §1 "Session persistence" for the format.
 *
 * The rust-sdk crypto/state store lives under files/matrix/data (+cache);
 * the session tokens are a properties file at files/matrix/account.properties.
 */
class MatrixAccount(private val context: Context) {

    /** Logged-in session tokens; the only shape that crosses into the engine. */
    data class MatrixSession(
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
            fun fromProperties(p: Properties): MatrixSession? {
                val userId = p.getProperty("userId") ?: return null
                val deviceId = p.getProperty("deviceId") ?: return null
                val accessToken = p.getProperty("accessToken") ?: return null
                val homeserverUrl = p.getProperty("homeserverUrl") ?: return null
                return MatrixSession(userId, deviceId, accessToken, p.getProperty("refreshToken"), homeserverUrl)
            }
        }
    }

    fun hasSession(): Boolean = sessionFile().exists()

    /**
     * Password login: builds a fresh client against [homeserverUrl], logs in,
     * persists the session and returns the client ready for sync. Throws on
     * failure (caller decides how to surface it); a failed login leaves no
     * persisted session.
     */
    suspend fun login(homeserverUrl: String, username: String, password: String, log: (String) -> Unit = {}): Client {
        val client = newClient(homeserverUrl)
        try {
            client.login(username, password, "SimpleUX", null)
        } catch (e: Exception) {
            runCatching { client.close() }
            throw e
        }
        saveSession(client, log)
        log("logged in as ${client.userId()} (device ${client.deviceId()})")
        return client
    }

    /**
     * Rebuild the client from the persisted session. Returns null when no
     * session exists or restore fails (login required); never throws.
     */
    suspend fun restore(log: (String) -> Unit = {}): Client? {
        val f = sessionFile()
        if (!f.exists()) return null
        val props = Properties().apply { f.inputStream().use { load(it) } }
        val session = MatrixSession.fromProperties(props) ?: run {
            log("stored session is incomplete, login required")
            return null
        }
        val client = newClient(session.homeserverUrl)
        try {
            client.restoreSession(session.toSdkSession())
        } catch (e: Exception) {
            runCatching { client.close() }
            log("restoreSession failed: ${e.message}")
            return null
        }
        log("session restored for ${client.userId()}")
        return client
    }

    /**
     * Server-side logout + full local wipe (session file, crypto store,
     * cache). Best effort: a network failure still wipes local state.
     */
    suspend fun logout(client: Client?, log: (String) -> Unit = {}) {
        client?.let { c ->
            runCatching { c.logout() }.onFailure { log("logout call failed: ${it.message}") }
            runCatching { c.close() }
        }
        sessionFile().delete()
        dataDir().deleteRecursively()
        cacheDir().deleteRecursively()
        log("logged out, local data wiped")
    }

    private fun sessionFile(): File = File(context.filesDir, "matrix/account.properties")

    private fun dataDir(): File = File(context.filesDir, "matrix/data").apply { mkdirs() }

    private fun cacheDir(): File = File(context.cacheDir, "matrix/cache").apply { mkdirs() }

    private suspend fun newClient(homeserverUrl: String): Client =
        ClientBuilder()
            .homeserverUrl(homeserverUrl)
            .sessionPaths(dataDir().absolutePath, cacheDir().absolutePath)
            .build()

    private fun saveSession(client: Client, log: (String) -> Unit) {
        val s = try {
            client.session()
        } catch (e: Exception) {
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
        sessionFile().parentFile?.mkdirs()
        sessionFile().outputStream().use { props.store(it, "matrix session") }
    }
}
