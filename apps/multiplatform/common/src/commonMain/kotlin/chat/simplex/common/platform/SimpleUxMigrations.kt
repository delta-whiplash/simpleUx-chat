package chat.simplex.common.platform

import chat.simplex.common.model.ChatController

/**
 * Fork migrations, run once per install at startup right after upstream's
 * [runMigrations]. Keyed off SimpleUxPrefs markers instead of upstream's
 * lastMigratedVersionCode: the release-train versionCode scheme
 * (36600000 + run*100) makes lastMigration-based gates unreachable for
 * exactly the rolling-install population they target (#60).
 */
fun runSimpleUxMigrations() {
  if (!SimpleUxPrefs.chatStyleDefaultsApplied()) {
    // Chat style: apply the SimpleUX design defaults (sharper corners, no
    // message tails) in the UI layer — the model defaults in SimpleXAPI.kt
    // stay untouched (byte-frozen per AGENTS.md §1). Only values still at the
    // upstream defaults are overridden; anything the user explicitly set in
    // Appearance afterwards is left alone.
    val prefs = ChatController.appPrefs
    if (prefs.chatItemRoundness.get() == 0.75f) {
      prefs.chatItemRoundness.set(1f)
    }
    if (prefs.chatItemTail.get()) {
      prefs.chatItemTail.set(false)
    }
    SimpleUxPrefs.setChatStyleDefaultsApplied(true)
  }
}
