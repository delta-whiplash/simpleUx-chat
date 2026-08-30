package chat.simplex.common.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual val simpleUxSettings: Settings by lazy {
  SharedPreferencesSettings(androidAppContext.getSharedPreferences("simpleux_prefs", Context.MODE_PRIVATE))
}
