<!-- labels: priority:medium, area:identity, drift:none -->
# [Identity] Android gaps: deep-link collision, debug suffix, FLAG_SECURE in debug

**Priority:** Medium · **Upstream drift risk:** None

**Files:**
- `apps/multiplatform/android/src/main/AndroidManifest.xml:71-109` (simplex scheme + `autoVerify` App Links on simplex.chat / smp*.simplex.im / smp*.simplexonflux.com)
- `apps/multiplatform/build.gradle.kts:25` (application-id suffix defaults to `""`)
- `apps/multiplatform/android/src/main/java/chat/simplex/app/MainActivity.kt:49`

**Problem:**
1. Deep-link handlers are identical to the official app — side-by-side installs fight over simplex.chat links (contrary to AGENTS.md §3.1 "avoid hijacking official app intents").
2. The debug application-id suffix is only set via `local.properties`; by default debug and release builds collide with each other.
3. `FLAG_SECURE` is skipped in all debug builds (`privacyProtectScreen.get() && !BuildConfig.DEBUG`) — the privacy screen protection setting silently does nothing in debug.

**Recommended fix:**
Decide and document the link strategy (chooser-friendly or per-AGENTS dedicated handling); default the debug suffix to `.debug`; gate FLAG_SECURE behind an explicit developer opt-out setting rather than build type.

**Acceptance criteria (DoD):**
- [ ] Link behavior documented and non-destructive vs official app
- [ ] Debug builds install alongside release by default
- [ ] FLAG_SECURE honored unless explicitly disabled
