<!-- labels: priority:high, area:drift, drift:blocking -->
# [Drift] Extract the SimpleUX chat-list shell out of upstream's ChatListView.kt

**Priority:** High · **Upstream drift risk:** **Blocking**

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt` (+644 committed, +794 uncommitted)

**Problem:**
In-place rewrite of upstream's highest-churn screen: 17+ interleaved hunks; upstream features deleted in place (`ToggleChatListCard`, `ChatListFeatureCards`, `NewChatSheetFloatingButton`, `oneHandUI` branches, scroll-persistence `DisposableEffect`); hosts a tab-navigation architecture (`SimpleUxTab` CHATS/CONTACTS/SETTINGS/PROFILE at lines ~217-249, ~1236+) replacing upstream's modal model. The uncommitted round already re-rewrites the committed round (`TelegramTopHeader` grew from 4 to 9 parameters).

**Upstream impact:**
`ChatListView.kt` is one of upstream's most frequently modified files — nearly every upstream touch will conflict. This is the fork's merge blocker.

**Recommended fix:**
Move all SimpleUX shell composables (`TelegramTopHeader`, `TelegramBottomIslandBar`, `IslandTabItem`, `SimpleUxTab` host, `ActiveContactsRail` wiring) to `views/ux/ChatListShell.kt`. Reduce the `ChatListView.kt` diff to a minimal scaffold swap at one call site. Keep unused upstream composables in place — dead-but-intact upstream code acts as a merge buffer.

**Acceptance criteria (DoD):**
- [ ] `ChatListView.kt` diff vs upstream < ~120 lines
- [ ] Shell lives in `views/ux/`
- [ ] Behavior unchanged
- [ ] Deleted upstream feature calls restored or consciously documented
