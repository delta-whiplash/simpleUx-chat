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

  fun chatStyleDefaultsApplied(settings: Settings = simpleUxSettings): Boolean =
    settings.getBoolean(CHAT_STYLE_DEFAULTS_KEY, false)

  fun setChatStyleDefaultsApplied(applied: Boolean, settings: Settings = simpleUxSettings) {
    settings.putBoolean(CHAT_STYLE_DEFAULTS_KEY, applied)
  }
}
