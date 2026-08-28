package chat.simplex.common.platform

import chat.simplex.common.views.helpers.createTmpFileAndDelete
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

private val starredChatsFile =
  File(desktopPlatform.configPath + File.separator + "starred-chats.properties")

private val starredChatsProps: Properties = Properties().also { props ->
  if (!starredChatsFile.exists()) return@also

  try {
    starredChatsFile.reader().use { props.load(it) }
  } catch (e: Exception) {
    Log.e(TAG, "Error reading starred chats file: ${e.stackTraceToString()}")
  }
}

private const val lock = "starredChatsSaver"

actual val starredChatsSettings: Settings by lazy {
  PropertiesSettings(starredChatsProps) {
    synchronized(lock) {
      try {
        createTmpFileAndDelete(preferencesTmpDir) { tmpFile ->
          tmpFile.writer().use { starredChatsProps.store(it, "") }
          starredChatsFile.parentFile.mkdirs()
          Files.move(tmpFile.toPath(), starredChatsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error saving starred chats file: ${e.stackTraceToString()}")
      }
    }
  }
}
