package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val starredChatsSettings: Settings by lazy {
  fileBackedSettings("starred-chats.properties", "starred chats")
}
