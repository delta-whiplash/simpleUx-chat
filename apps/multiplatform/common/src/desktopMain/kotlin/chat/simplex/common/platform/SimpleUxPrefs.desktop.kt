package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val simpleUxSettings: Settings by lazy {
  fileBackedSettings("simpleux-prefs.properties", "simpleux prefs")
}
