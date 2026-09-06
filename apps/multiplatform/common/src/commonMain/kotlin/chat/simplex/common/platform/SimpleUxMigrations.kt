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
    // message tails) in the UI layer - the model defaults in SimpleXAPI.kt
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

  if (!SimpleUxPrefs.remoteDiscoverySeedApplied()) {
    // Linking level 1 (#127): auto-search for known desktops when the "Use from
    // desktop" screen opens. The upstream default (off) lives in the byte-frozen
    // model, so seed it ON once here; the "Discover via local network" toggle in
    // Linked desktop options remains the opt-out.
    val discoveryPrefs = ChatController.appPrefs
    if (!discoveryPrefs.connectRemoteViaMulticast.get()) {
      discoveryPrefs.connectRemoteViaMulticast.set(true)
    }
    SimpleUxPrefs.setRemoteDiscoverySeedApplied(true)
  }
}
