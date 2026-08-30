package chat.simplex.common.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual val updaterSettings: Settings by lazy {
  SharedPreferencesSettings(androidAppContext.getSharedPreferences("simpleux_updater", Context.MODE_PRIVATE))
}
