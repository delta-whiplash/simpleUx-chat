package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val starredChatsSettings: Settings by lazy {
  sharedPrefsSettings("simpleux_starred_chats")
}
