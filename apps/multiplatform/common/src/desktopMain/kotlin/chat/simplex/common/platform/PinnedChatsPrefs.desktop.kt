package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val pinnedChatsSettings: Settings by lazy {
  fileBackedSettings("pinned-chats.properties", "pinned chats")
}
