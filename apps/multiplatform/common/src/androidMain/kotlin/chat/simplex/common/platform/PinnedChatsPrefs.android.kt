package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val pinnedChatsSettings: Settings by lazy {
  sharedPrefsSettings("simpleux_pinned_chats")
}
