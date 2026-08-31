package chat.simplex.common.views.ux.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import chat.simplex.common.helpers.APPLICATION_ID
import chat.simplex.common.platform.Log
import chat.simplex.common.platform.androidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

actual fun provideAppUpdateInstaller(): AppUpdateInstaller = AndroidAppUpdateInstaller

/**
 * Downloads the release APK into the app cache and hands it to the system
 * package installer. The FileProvider authority is computed at runtime from
 * [APPLICATION_ID] (mirroring Utils.android.kt / QuickCameraSheet.kt), which
 * resolves the manifest placeholder `${provider_authorities}` including any
 * debug suffix - never hard-coded here.
 */
object AndroidAppUpdateInstaller : AppUpdateInstaller {
  private const val TAG = "AppUpdateInstaller"
  private const val APK_CACHE_SUBDIR = "updates"
  private const val PROGRESS_REPORT_STEP_BYTES = 256L * 1024

  override fun canInstallPackages(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
      androidAppContext.packageManager.canRequestPackageInstalls()

  override fun openInstallPermissionSettings() {
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
      data = Uri.parse("package:${androidAppContext.packageName}")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      androidAppContext.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Could not open install-permission settings: ${e.stackTraceToString()}")
    }
  }

  override suspend fun downloadApk(
    candidate: AppUpdateCandidate,
    onProgress: (receivedBytes: Long, totalBytes: Long?) -> Unit,
  ): String = withContext(Dispatchers.IO) {
    val dir = File(androidAppContext.cacheDir, APK_CACHE_SUBDIR).apply { mkdirs() }
    val file = File(dir, candidate.apkName)
    val conn = URL(candidate.downloadUrl).openConnection() as HttpURLConnection
    try {
      conn.connectTimeout = 8000
      conn.readTimeout = 30_000
      conn.requestMethod = "GET"
      conn.instanceFollowRedirects = true
      conn.setRequestProperty("User-Agent", "SimpleUX-Chat/1.0")
      if (conn.responseCode != HttpURLConnection.HTTP_OK) {
        error("HTTP ${conn.responseCode} downloading ${candidate.apkName}")
      }
      val total = conn.contentLengthLong.takeIf { it > 0 } ?: candidate.sizeBytes
      conn.inputStream.use { input ->
        file.outputStream().use { output ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
          var received = 0L
          var lastReported = 0L
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            received += read
            if (received - lastReported >= PROGRESS_REPORT_STEP_BYTES) {
              lastReported = received
              onProgress(received, total)
            }
          }
          onProgress(received, total)
          if (received <= 0L) error("Empty download for ${candidate.apkName}")
        }
      }
      file.absolutePath
    } catch (e: Throwable) {
      file.delete()
      throw e
    } finally {
      conn.disconnect()
    }
  }

  override fun installApk(filePath: String) {
    val file = File(filePath)
    val uri = FileProvider.getUriForFile(androidAppContext, "$APPLICATION_ID.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    androidAppContext.startActivity(intent)
  }
}
