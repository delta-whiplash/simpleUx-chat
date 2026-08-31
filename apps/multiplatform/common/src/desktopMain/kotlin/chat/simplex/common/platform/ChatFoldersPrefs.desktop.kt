package chat.simplex.common.platform

import chat.simplex.common.views.helpers.createTmpFileAndDelete
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

private val chatFoldersFile =
  File(desktopPlatform.configPath + File.separator + "chat-folders.properties")

private val chatFoldersProps: Properties = Properties().also { props ->
  if (!chatFoldersFile.exists()) return@also

  try {
    chatFoldersFile.reader().use { props.load(it) }
  } catch (e: Exception) {
    println("Error reading chat folders file: ${e.stackTraceToString()}")
  }
}

private const val lock = "chatFoldersSaver"

actual val chatFoldersSettings: Settings by lazy {
  PropertiesSettings(chatFoldersProps) {
    synchronized(lock) {
      try {
        createTmpFileAndDelete(preferencesTmpDir) { tmpFile ->
          tmpFile.writer().use { chatFoldersProps.store(it, "") }
          chatFoldersFile.parentFile.mkdirs()
          Files.move(tmpFile.toPath(), chatFoldersFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      } catch (e: Exception) {
        println("Error saving chat folders file: ${e.stackTraceToString()}")
      }
    }
  }
}
