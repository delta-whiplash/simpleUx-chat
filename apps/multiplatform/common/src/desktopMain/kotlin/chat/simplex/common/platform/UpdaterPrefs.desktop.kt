package chat.simplex.common.platform

import chat.simplex.common.views.helpers.createTmpFileAndDelete
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

private val updaterPrefsFile =
  File(desktopPlatform.configPath + File.separator + "updater.properties")

private val updaterProps: Properties = Properties().also { props ->
  if (!updaterPrefsFile.exists()) return@also

  try {
    updaterPrefsFile.reader().use { props.load(it) }
  } catch (e: Exception) {
    Log.e(TAG, "Error reading updater prefs file: ${e.stackTraceToString()}")
  }
}

private const val lock = "updaterPrefsSaver"

actual val updaterSettings: Settings by lazy {
  PropertiesSettings(updaterProps) {
    synchronized(lock) {
      try {
        createTmpFileAndDelete(preferencesTmpDir) { tmpFile ->
          tmpFile.writer().use { updaterProps.store(it, "") }
          updaterPrefsFile.parentFile.mkdirs()
          Files.move(tmpFile.toPath(), updaterPrefsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error saving updater prefs file: ${e.stackTraceToString()}")
      }
    }
  }
}
