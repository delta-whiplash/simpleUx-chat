package chat.simplex.common.platform

import com.russhwolf.settings.Settings

// SimpleUX cross-cutting flags & one-time migration markers, backed by their
// own moko multiplatform-settings instance. Kept separate from AppPreferences
// (model/SimpleXAPI.kt is byte-frozen): fork migrations key off these markers,
// never off upstream's lastMigratedVersionCode (whose value is dominated by
// the release-train versionCode scheme).
expect val simpleUxSettings: Settings

object SimpleUxPrefs {
  private const val CHAT_STYLE_DEFAULTS_KEY = "simpleux.migrations.chatStyleDefaults"
  private const val REMOTE_DISCOVERY_SEED_KEY = "simpleux.migrations.remoteDiscoverySeed"
  private const val REMOTE_HOST_AUTOSTART_KEY = "simpleux.remoteHostAutostart"

  fun chatStyleDefaultsApplied(settings: Settings = simpleUxSettings): Boolean =
    settings.getBoolean(CHAT_STYLE_DEFAULTS_KEY, false)

  fun setChatStyleDefaultsApplied(applied: Boolean, settings: Settings = simpleUxSettings) {
    settings.putBoolean(CHAT_STYLE_DEFAULTS_KEY, applied)
  }

  fun remoteDiscoverySeedApplied(settings: Settings = simpleUxSettings): Boolean =
    settings.getBoolean(REMOTE_DISCOVERY_SEED_KEY, false)

  fun setRemoteDiscoverySeedApplied(applied: Boolean, settings: Settings = simpleUxSettings) {
    settings.putBoolean(REMOTE_DISCOVERY_SEED_KEY, applied)
  }

  /** Desktop (#127): listen for linked mobiles at app startup. SimpleUX default: on. */
  fun remoteHostAutostart(settings: Settings = simpleUxSettings): Boolean =
    settings.getBoolean(REMOTE_HOST_AUTOSTART_KEY, true)

  fun setRemoteHostAutostart(enabled: Boolean, settings: Settings = simpleUxSettings) {
    settings.putBoolean(REMOTE_HOST_AUTOSTART_KEY, enabled)
  }
}
