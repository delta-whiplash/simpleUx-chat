package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val simpleUxSettings: Settings by lazy {
  sharedPrefsSettings("simpleux_prefs")
}
