package chat.simplex.common.views.ux

import chat.simplex.common.platform.settings

/**
 * UI-layer privacy defaults that intentionally differ from the frozen model layer.
 *
 * The model layer is byte-frozen (see AGENTS.md §1) and declares the "Remove link
 * tracking" preference with a stored default of `false`
 * (`model/SimpleXAPI.kt`, `privacySanitizeLinks = mkBoolPreference(..., false)`;
 * its key constant `SHARED_PREFS_PRIVACY_SANITIZE_LINKS` is private). SimpleUX flips
 * the *effective* default here instead of editing the model: when the underlying
 * store has no explicitly written value yet - fresh install, toggle never touched,
 * no settings import, no remote-host sync - link sanitization is ON. Once any
 * explicit value exists (user toggle, settings import, remote sync - all write via
 * `SharedPreference.set`), it is honored as-is, including a deliberate opt-out.
 */

// Must stay in sync with model/SimpleXAPI.kt `SHARED_PREFS_PRIVACY_SANITIZE_LINKS`
// (frozen file - do not edit there; duplicated here because the constant is private).
private const val PRIVACY_SANITIZE_LINKS_KEY = "PrivacySanitizeLinks"

/**
 * Effective value of "Remove link tracking": an explicitly stored value always wins;
 * an unset key means ON. Composables should pass the value read from
 * `pref.state.value` so the toggle re-renders immediately; plain call sites can pass
 * `pref.get()`.
 */
fun sanitizeLinksEffective(stored: Boolean): Boolean =
  stored || !settings.hasKey(PRIVACY_SANITIZE_LINKS_KEY)
