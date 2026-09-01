package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val updaterSettings: Settings by lazy {
  fileBackedSettings("updater.properties", "updater prefs")
}
