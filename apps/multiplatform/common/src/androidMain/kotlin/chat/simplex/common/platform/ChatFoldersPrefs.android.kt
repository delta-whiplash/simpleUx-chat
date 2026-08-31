package chat.simplex.common.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual val chatFoldersSettings: Settings by lazy {
  SharedPreferencesSettings(androidAppContext.getSharedPreferences("simpleux_chat_folders", Context.MODE_PRIVATE))
}
