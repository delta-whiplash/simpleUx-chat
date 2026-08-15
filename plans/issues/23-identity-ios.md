<!-- labels: priority:medium, area:identity, drift:none -->
# [Identity] iOS coexistence not started (bundle id still chat.simplex.app)

**Priority:** Medium · **Upstream drift risk:** None

**Files:**
- `apps/ios/**` — `project.pbxproj` (`PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.app` at multiple targets), `Info.plist`

**Problem:**
AGENTS.md §3.2 requires bundle id `chat.simplex.ux`, app groups `group.chat.simplex.ux`, display name SimpleUX. The fork has not touched the iOS app at all — installing SimpleUX iOS alongside official SimpleX is currently impossible (identifier collision).

**Recommended fix:**
Rename bundle identifiers across targets, set dedicated app groups / keychain access groups, display name SimpleUX. Requires macOS/Xcode.

**Acceptance criteria (DoD):**
- [ ] Bundle id `chat.simplex.ux` on all targets
- [ ] Dedicated app groups/keychain groups
- [ ] Side-by-side install with the official App Store app verified
