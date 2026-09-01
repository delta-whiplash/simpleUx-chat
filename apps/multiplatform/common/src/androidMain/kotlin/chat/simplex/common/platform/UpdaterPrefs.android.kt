package chat.simplex.common.platform

import com.russhwolf.settings.Settings

actual val updaterSettings: Settings by lazy {
  sharedPrefsSettings("simpleux_updater")
}
