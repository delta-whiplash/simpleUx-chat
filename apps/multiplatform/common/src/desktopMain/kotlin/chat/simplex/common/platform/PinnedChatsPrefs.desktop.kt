package chat.simplex.common.platform

import chat.simplex.common.views.helpers.createTmpFileAndDelete
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

private val pinnedChatsFile =
  File(desktopPlatform.configPath + File.separator + "pinned-chats.properties")

private val pinnedChatsProps: Properties = Properties().also { props ->
  if (!pinnedChatsFile.exists()) return@also

  try {
    pinnedChatsFile.reader().use { props.load(it) }
  } catch (e: Exception) {
    Log.e(TAG, "Error reading pinned chats file: ${e.stackTraceToString()}")
  }
}

private const val lock = "pinnedChatsSaver"

actual val pinnedChatsSettings: Settings by lazy {
  PropertiesSettings(pinnedChatsProps) {
    synchronized(lock) {
      try {
        createTmpFileAndDelete(preferencesTmpDir) { tmpFile ->
          tmpFile.writer().use { pinnedChatsProps.store(it, "") }
          pinnedChatsFile.parentFile.mkdirs()
          Files.move(tmpFile.toPath(), pinnedChatsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error saving pinned chats file: ${e.stackTraceToString()}")
      }
    }
  }
}
