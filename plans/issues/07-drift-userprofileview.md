<!-- labels: priority:medium, area:drift, drift:high -->
# [Drift] UserProfileView.kt: reduce rewrite surface & restore lost upstream behaviors

**Priority:** Medium · **Upstream drift risk:** High

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/UserProfileView.kt` (+685 lines uncommitted)

**Problem:**
Full in-place rewrite of the profile editor (`ProfileTextBox` replaces `ProfileNameField`, new button row, new "Connexions & Liens" section). Functional parity is mostly kept (image pick/crop/resize, save, unsaved-changes guard — verified) but the rewrite dropped: the privacy explainer copy (`your_profile_is_stored_on_device…`), `ModalView` wrapper (desktop center-panel/modal sizing), one-hand UI support, and `ProfileNameField`'s IME/KeyboardOptions. Copy is mixed hardcoded French/English.

**Upstream impact:**
Upstream evolves profile editing (addresses, display names, image handling) — a full rewrite of this screen conflicts on every upstream change and silently loses new upstream features.

**Recommended fix:**
Wrap instead of replace: rebase the restyling on the upstream structure. Restore `ModalView`, the privacy explainer, and keyboard options. Localize all copy.

**Acceptance criteria (DoD):**
- [ ] Diff vs upstream is restyle-only (no structural rewrites)
- [ ] Desktop modal sizing works
- [ ] Privacy explainer restored
- [ ] No mixed-language hardcoded strings
