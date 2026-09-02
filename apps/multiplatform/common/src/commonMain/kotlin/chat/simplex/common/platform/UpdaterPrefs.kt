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
private const val AUTO_CHECK_LAST_AT_KEY = "simpleux.updater.autoCheckLastAt"
private const val NOTICE_DISMISSED_VERSION_KEY = "simpleux.updater.noticeDismissedVersion"

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

  /** When the launch notice last checked for updates - gates the 24h re-check (#109) */
  fun autoCheckLastAt(settings: Settings = updaterSettings): Long =
    settings.getLong(AUTO_CHECK_LAST_AT_KEY, 0L)

  fun setAutoCheckLastAt(atMillis: Long, settings: Settings = updaterSettings) {
    settings.putLong(AUTO_CHECK_LAST_AT_KEY, atMillis)
  }

  /** Version the user dismissed on the launch notice - not re-offered until a newer one (#109) */
  fun noticeDismissedVersion(settings: Settings = updaterSettings): String? =
    settings.getStringOrNull(NOTICE_DISMISSED_VERSION_KEY)

  fun setNoticeDismissedVersion(version: String?, settings: Settings = updaterSettings) {
    if (version == null) settings.remove(NOTICE_DISMISSED_VERSION_KEY)
    else settings.putString(NOTICE_DISMISSED_VERSION_KEY, version)
  }
}
