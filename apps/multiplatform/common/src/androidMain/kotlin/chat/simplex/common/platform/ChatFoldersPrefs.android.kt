package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val chatFoldersSettings: Settings by lazy {
  sharedPrefsSettings("simpleux_chat_folders")
}
