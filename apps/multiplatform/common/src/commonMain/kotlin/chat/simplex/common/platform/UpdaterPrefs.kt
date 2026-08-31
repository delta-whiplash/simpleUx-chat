package chat.simplex.common.platform

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get

// SimpleUX updater preferences (#79), backed by their own moko
// multiplatform-settings instance - kept separate from AppPreferences
// (model/SimpleXAPI.kt is byte-frozen). The auto check defaults to false:
// a privacy messenger must not contact api.github.com unless asked.
expect val updaterSettings: Settings

private const val AUTO_CHECK_STABLE_KEY = "simpleux.updater.autoCheckStable"
private const val UPDATE_CHANNEL_KEY = "simpleux.updater.channel"

/** Update channel selection - stable only or rolling pre-releases (#97) */
enum class UpdateChannel(val key: String) {
  STABLE("stable"),
  ROLLING("rolling");

  companion object {
    fun fromKey(key: String?): UpdateChannel = 
      values().find { it.key == key } ?: STABLE
  }
}

object UpdaterPrefs {
  fun autoCheckEnabled(settings: Settings = updaterSettings): Boolean =
    settings.getBoolean(AUTO_CHECK_STABLE_KEY, false)

  fun setAutoCheckEnabled(enabled: Boolean, settings: Settings = updaterSettings) {
    settings.putBoolean(AUTO_CHECK_STABLE_KEY, enabled)
  }

  /** Get selected update channel - defaults to STABLE for safety (#97) */
  fun updateChannel(settings: Settings = updaterSettings): UpdateChannel =
    UpdateChannel.fromKey(settings.getStringOrNull(UPDATE_CHANNEL_KEY))

  /** Set update channel preference (#97) */
  fun setUpdateChannel(channel: UpdateChannel, settings: Settings = updaterSettings) {
    settings.putString(UPDATE_CHANNEL_KEY, channel.key)
  }
}
