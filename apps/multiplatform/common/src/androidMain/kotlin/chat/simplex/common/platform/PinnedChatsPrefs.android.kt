package chat.simplex.common.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual val pinnedChatsSettings: Settings by lazy {
  SharedPreferencesSettings(androidAppContext.getSharedPreferences("simpleux_pinned_chats", Context.MODE_PRIVATE))
}
