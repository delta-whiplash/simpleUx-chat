package chat.simplex.common.views.ux.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import chat.simplex.common.platform.Log
import chat.simplex.common.platform.UpdateChannel
import chat.simplex.common.platform.UpdaterPrefs

/** HARD-PINNED to the fork repo - never upstream simplex-chat (pinned by AppUpdaterTest). */
const val RELEASES_API_URL = "https://api.github.com/repos/delta-whiplash/simpleUx-chat/releases?per_page=10"

/** Project home, opened when the user taps the version row in VersionInfoView. */
const val FORK_PROJECT_URL = "https://github.com/delta-whiplash/simpleUx-chat"

private const val TAG = "AppUpdater"

private val releasesJsonParser = Json {
  ignoreUnknownKeys = true
  isLenient = true
}

private val APK_ASSET_REGEX = Regex("simplex-ux-.*-arm64-v8a\\.apk")
// Extracts the fork version from an asset name - rolling releases carry a floating
// "rolling" tag, so the version only lives in the asset name ("7.0.366-ux.5" below).
private val APK_VERSION_REGEX = Regex("simplex-ux-(.+)-arm64-v8a\\.apk")

enum class AppUpdateVersionComparison { NEWER, SAME_OR_OLDER, NOT_A_VERSION }

/**
 * A parsed fork version: upstream base ("7.0.366" in "7.0.366-ux.5") plus the fork counter (5).
 * Comparison is element-wise on the base tuple (zero-padded) first, then on the fork counter.
 */
internal data class AppUpdateVersion(val base: List<Int>, val forkCounter: Int) : Comparable<AppUpdateVersion> {
  override fun compareTo(other: AppUpdateVersion): Int {
    for (i in 0 until maxOf(base.size, other.base.size)) {
      val compared = base.getOrElse(i) { 0 }.compareTo(other.base.getOrElse(i) { 0 })
      if (compared != 0) return compared
    }
    return forkCounter.compareTo(other.forkCounter)
  }
}

/**
 * Parses "7.0.366-ux.5" / "v7.0.366-ux.5". Anything else - the floating "rolling" tag,
 * versions without the "-ux." counter, malformed numbers - yields null so the candidate
 * is never proposed as an update.
 */
internal fun parseAppUpdateVersion(raw: String): AppUpdateVersion? {
  val stripped = raw.trim().removePrefix("v").removePrefix("V")
  val separator = stripped.indexOf("-ux.")
  if (separator < 0) return null
  val baseParts = stripped.substring(0, separator).split(".")
  if (baseParts.isEmpty() || baseParts.any { it.isEmpty() || it.any { c -> !c.isDigit() } }) return null
  val counter = stripped.substring(separator + 4)
  if (counter.isEmpty() || counter.any { !it.isDigit() }) return null
  return AppUpdateVersion(baseParts.map { it.toInt() }, counter.toInt())
}

/**
 * Language-neutral version comparison for the updater: NEWER only when [candidate] is a
 * parseable fork version strictly greater than [current]; NOT_A_VERSION means "never
 * propose an update from this tag". A [current] version without the "-ux." counter
 * (local builds and pre-#72 installs) still has a meaningful upstream base - its fork
 * counter is treated as 0 so it can be updated.
 */
fun compareAppUpdateVersions(current: String, candidate: String): AppUpdateVersionComparison {
  val candidateVersion = parseAppUpdateVersion(candidate) ?: return AppUpdateVersionComparison.NOT_A_VERSION
  val currentVersion = parseAppUpdateVersion(current)
    ?: parseVersionBase(current)?.let { AppUpdateVersion(it, 0) }
    ?: return AppUpdateVersionComparison.NOT_A_VERSION
  return if (candidateVersion > currentVersion) AppUpdateVersionComparison.NEWER else AppUpdateVersionComparison.SAME_OR_OLDER
}

private fun parseVersionBase(raw: String): List<Int>? {
  val parts = raw.trim().removePrefix("v").removePrefix("V").split(".")
  if (parts.isEmpty() || parts.any { it.isEmpty() || it.any { c -> !c.isDigit() } }) return null
  return parts.map { it.toInt() }
}

data class AppUpdateCandidate(
  val tagName: String,
  // Comparable fork version: the tag when it parses ("v7.0.366-ux.2"), otherwise derived
  // from the asset name (rolling releases). Unparseable either way => selection returns null.
  val version: String,
  val apkName: String,
  val downloadUrl: String,
  val sizeBytes: Long? = null,
)

/**
 * Pure structural selection from the GitHub releases JSON (unit-tested offline).
 * 
 * @param channel The update channel to use (STABLE or ROLLING)
 * @param currentVersion The current app version for comparison
 * Returns null when no release matches the channel rules or no entry carries a
 * `simplex-ux-...-arm64-v8a.apk` asset.
 */
internal fun selectAppUpdateRelease(rawJson: String, channel: UpdateChannel, currentVersion: String): AppUpdateCandidate? {
  return try {
    val releases = releasesJsonParser.parseToJsonElement(rawJson).jsonArray
    
    // Filter releases by channel and parse valid candidates
    val candidates = releases.asSequence()
      .map { it.jsonObject }
      .filter { release ->
        when (channel) {
          UpdateChannel.STABLE -> release["prerelease"]?.jsonPrimitive?.content == "false"
          UpdateChannel.ROLLING -> true // Include all releases for rolling channel
        }
      }
      .mapNotNull { release -> parseReleaseCandidate(release) }
      .toList()
    
    // Find the best candidate by version comparison
    val currentParsed = parseAppUpdateVersion(currentVersion)
      ?: parseVersionBase(currentVersion)?.let { AppUpdateVersion(it, 0) }
    
    candidates
      .filter { candidate ->
        // Only consider candidates newer than current version
        val candidateParsed = parseAppUpdateVersion(candidate.version)
        if (candidateParsed == null || currentParsed == null) return@filter false
        candidateParsed > currentParsed
      }
      .maxByOrNull { parseAppUpdateVersion(it.version)?.let { v -> v } ?: AppUpdateVersion(listOf(0), 0) }
      
  } catch (e: Exception) {
    Log.w(TAG, "Failed to parse releases JSON: ${e.message}")
    null
  }
}

/** Parse a single release into a candidate, or null if invalid */
private fun parseReleaseCandidate(release: kotlinx.serialization.json.JsonObject): AppUpdateCandidate? {
  val tagName = release["tag_name"]?.jsonPrimitive?.content ?: return null
  val assets = release["assets"]?.jsonArray ?: return null
  val asset = assets.asSequence()
    .map { it.jsonObject }
    .firstOrNull { APK_ASSET_REGEX.matches(it["name"]?.jsonPrimitive?.content ?: "") }
    ?: return null
  val apkName = asset["name"]?.jsonPrimitive?.content ?: return null
  val version = if (parseAppUpdateVersion(tagName) != null) tagName.removePrefix("v")
    else APK_VERSION_REGEX.find(apkName)?.groupValues?.get(1) ?: return null
  return AppUpdateCandidate(
    tagName = tagName,
    version = version,
    apkName = apkName,
    downloadUrl = asset["browser_download_url"]?.jsonPrimitive?.content ?: return null,
    sizeBytes = asset["size"]?.jsonPrimitive?.content?.toLongOrNull()
  )
}

sealed interface AppUpdateState {
  data object Idle : AppUpdateState
  data object Checking : AppUpdateState
  data class UpToDate(val version: String) : AppUpdateState
  data class UpdateAvailable(val candidate: AppUpdateCandidate) : AppUpdateState
  data class Downloading(val receivedBytes: Long, val totalBytes: Long?) : AppUpdateState
  data class ReadyToInstall(val filePath: String, val candidate: AppUpdateCandidate) : AppUpdateState
  data class InstallPermissionNeeded(val filePath: String, val candidate: AppUpdateCandidate) : AppUpdateState
  data object Failed : AppUpdateState
}

/**
 * Platform bridge for the parts of the updater that cannot live in commonMain:
 * APK download target, install-permission check, and firing the system installer.
 */
interface AppUpdateInstaller {
  /** Downloads [candidate], reporting (receivedBytes, totalBytes) progress; returns the file path. */
  suspend fun downloadApk(
    candidate: AppUpdateCandidate,
    onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
  ): String

  /** Whether this app may install packages (Android setting). */
  fun canInstallPackages(): Boolean

  /** Hands a downloaded APK to the system installer. */
  fun installApk(filePath: String)

  /** Opens the system screen that grants the install permission for this app. */
  fun openInstallPermissionSettings()
}

expect fun provideAppUpdateInstaller(): AppUpdateInstaller

/**
 * Orchestrates the manual check → download → install flow for issue #79.
 * Everything is injected (scope, installer, running version) so the class is
 * constructible in isolation.
 */
class AppUpdater(
  private val scope: CoroutineScope,
  private val installer: AppUpdateInstaller,
  private val currentVersion: String,
) {
  private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
  val state: StateFlow<AppUpdateState> = _state.asStateFlow()

  fun checkForUpdates() {
    when (_state.value) {
      is AppUpdateState.Checking, is AppUpdateState.Downloading -> return
      else -> {}
    }
    _state.value = AppUpdateState.Checking
    val channel = UpdaterPrefs.updateChannel()
    scope.launch(Dispatchers.IO) {
      try {
        val rawJson = fetchReleasesJson()
        val candidate = rawJson?.let { selectAppUpdateRelease(it, channel, currentVersion) }
        _state.value = when {
          rawJson == null || candidate == null -> AppUpdateState.UpToDate(currentVersion)
          else -> AppUpdateState.UpdateAvailable(candidate)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Update check failed: ${e.stackTraceToString()}")
        _state.value = AppUpdateState.Failed
      }
    }
  }

  /**
   * Silent, opt-in auto check (#79): the user must have enabled it in the
   * updater section (default OFF - zero network at startup otherwise). Only
   * stable releases are considered for auto-check; any failure or
   * "already current" outcome is swallowed so a background check never
   * bothers the user. A strictly newer stable is pushed both into [state]
   * (visible if the updater section is open) and to [onUpdateAvailable].
   */
  fun autoCheckForUpdates(onUpdateAvailable: (AppUpdateCandidate) -> Unit) {
    if (_state.value != AppUpdateState.Idle) return
    scope.launch(Dispatchers.IO) {
      runCatching {
        val rawJson = fetchReleasesJson() ?: return@launch
        // Auto-check always uses STABLE channel regardless of user preference
        val candidate = selectAppUpdateRelease(rawJson, UpdateChannel.STABLE, currentVersion) ?: return@launch
        _state.value = AppUpdateState.UpdateAvailable(candidate)
        onUpdateAvailable(candidate)
      }
    }
  }

  fun downloadUpdate() {
    val candidate = (_state.value as? AppUpdateState.UpdateAvailable)?.candidate ?: return
    _state.value = AppUpdateState.Downloading(0, candidate.sizeBytes)
    scope.launch {
      try {
        val filePath = installer.downloadApk(candidate) { received, total ->
          _state.value = AppUpdateState.Downloading(received, total)
        }
        _state.value = if (installer.canInstallPackages())
          AppUpdateState.ReadyToInstall(filePath, candidate)
        else
          AppUpdateState.InstallPermissionNeeded(filePath, candidate)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "APK download failed: ${e.stackTraceToString()}")
        _state.value = AppUpdateState.Failed
      }
    }
  }

  /** Install button: (re-)checks the install permission, offering the system settings when missing. */
  fun installUpdate() {
    when (val s = _state.value) {
      is AppUpdateState.ReadyToInstall -> installer.installApk(s.filePath)
      is AppUpdateState.InstallPermissionNeeded ->
        if (installer.canInstallPackages()) {
          _state.value = AppUpdateState.ReadyToInstall(s.filePath, s.candidate)
          installer.installApk(s.filePath)
        } else {
          installer.openInstallPermissionSettings()
        }
      else -> {}
    }
  }

  private fun fetchReleasesJson(): String? {
    return try {
      val conn = URL(RELEASES_API_URL).openConnection() as HttpURLConnection
      conn.connectTimeout = 8000
      conn.readTimeout = 10_000
      conn.requestMethod = "GET"
      conn.setRequestProperty("Accept", "application/vnd.github+json")
      conn.setRequestProperty("User-Agent", "SimpleUX-Chat/1.0")
      if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        conn.inputStream.bufferedReader().use { it.readText() }
      } else {
        Log.w(TAG, "HTTP ${conn.responseCode} fetching releases")
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to fetch releases: ${e.message}")
      null
    }
  }
}
