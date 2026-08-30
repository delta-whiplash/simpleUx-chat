package chat.simplex.common.platform

import chat.simplex.common.views.helpers.createTmpFileAndDelete
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

private val simpleUxPrefsFile =
  File(desktopPlatform.configPath + File.separator + "simpleux-prefs.properties")

private val simpleUxProps: Properties = Properties().also { props ->
  if (!simpleUxPrefsFile.exists()) return@also

  try {
    simpleUxPrefsFile.reader().use { props.load(it) }
  } catch (e: Exception) {
    Log.e(TAG, "Error reading simpleux prefs file: ${e.stackTraceToString()}")
  }
}

private const val lock = "simpleUxPrefsSaver"

actual val simpleUxSettings: Settings by lazy {
  PropertiesSettings(simpleUxProps) {
    synchronized(lock) {
      try {
        createTmpFileAndDelete(preferencesTmpDir) { tmpFile ->
          tmpFile.writer().use { simpleUxProps.store(it, "") }
          simpleUxPrefsFile.parentFile.mkdirs()
          Files.move(tmpFile.toPath(), simpleUxPrefsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error saving simpleux prefs file: ${e.stackTraceToString()}")
      }
    }
  }
}
