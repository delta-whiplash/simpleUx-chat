<!-- labels: priority:high, area:drift, drift:high -->
# [Drift] Revert model-layer edit: oneHandUI default flipped in SimpleXAPI.kt

**Priority:** High · **Upstream drift risk:** High

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt:269`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/views/usersettings/Appearance.android.kt:108`
- Stranded reads: `views/TerminalView.kt:78`, `views/chatlist/TagListView.android.kt:55`, `views/chat/item/ChatItemInfoView.kt:513`, `views/chat/ChatView.kt:1001-1002`

**Problem:**
The fork's only model-layer change flips a shared preference default (`oneHandUI` true→false) while the setting's UI was removed from Appearance and four files still read the preference — a half-applied removal with semantic ripple across all platforms (users who enabled it pre-fork have a stuck, un-toggleable preference).

**Upstream impact:**
`SimpleXAPI.kt` is shared by every platform and actively developed upstream (the 22 commits on `upstream/master` since the fork already touch this file — the only overlapping file between fork and upstream drift). Keeping the fork's line guarantees a recurring conflict point in the widest-shared file of the codebase.

**Recommended fix:**
Restore the upstream default (`true`). The SimpleUX shell ignores the preference at its single scaffold site. If one-hand UI must be removed, finish the removal in the UI layer only.

**Acceptance criteria (DoD):**
- [ ] `SimpleXAPI.kt` byte-identical to upstream
- [ ] No stranded half-behavior reads of `oneHandUI`
- [ ] SimpleUX layout unaffected by the pref's value
