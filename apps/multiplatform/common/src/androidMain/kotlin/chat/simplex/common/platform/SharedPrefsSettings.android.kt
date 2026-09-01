package chat.simplex.common.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

internal fun sharedPrefsSettings(name: String): Settings =
  SharedPreferencesSettings(androidAppContext.getSharedPreferences(name, Context.MODE_PRIVATE))
