package chat.simplex.common.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual val starredChatsSettings: Settings by lazy {
  SharedPreferencesSettings(androidAppContext.getSharedPreferences("simpleux_starred_chats", Context.MODE_PRIVATE))
}
