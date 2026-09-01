package chat.simplex.common.platform

import chat.simplex.common.views.helpers.createTmpFileAndDelete
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

// One properties file per SimpleUX preference store under the config directory,
// loaded once and written atomically through a per-store saver lock.
internal fun fileBackedSettings(fileName: String, logLabel: String): Settings {
  val file = File(desktopPlatform.configPath + File.separator + fileName)
  val props: Properties = Properties().also { props ->
    if (!file.exists()) return@also

    try {
      file.reader().use { props.load(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error reading $logLabel file: ${e.stackTraceToString()}")
    }
  }

  val saverLock = Any()
  return PropertiesSettings(props) {
    synchronized(saverLock) {
      try {
        createTmpFileAndDelete(preferencesTmpDir) { tmpFile ->
          tmpFile.writer().use { props.store(it, "") }
          file.parentFile.mkdirs()
          Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error saving $logLabel file: ${e.stackTraceToString()}")
      }
    }
  }
}
