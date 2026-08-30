package chat.simplex.common.platform

import com.russhwolf.settings.Settings

// SimpleUX updater preferences (#79), backed by their own moko
// multiplatform-settings instance — kept separate from AppPreferences
// (model/SimpleXAPI.kt is byte-frozen). The auto check defaults to false:
// a privacy messenger must not contact api.github.com unless asked.
expect val updaterSettings: Settings

private const val AUTO_CHECK_STABLE_KEY = "simpleux.updater.autoCheckStable"

object UpdaterPrefs {
  fun autoCheckEnabled(settings: Settings = updaterSettings): Boolean =
    settings.getBoolean(AUTO_CHECK_STABLE_KEY, false)

  fun setAutoCheckEnabled(enabled: Boolean, settings: Settings = updaterSettings) {
    settings.putBoolean(AUTO_CHECK_STABLE_KEY, enabled)
  }
}
