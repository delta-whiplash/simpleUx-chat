package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val chatFoldersSettings: Settings by lazy {
  fileBackedSettings("chat-folders.properties", "chat folders")
}
