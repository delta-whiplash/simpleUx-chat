<!-- labels: priority:high, area:ux, drift:none -->
# [Regression] Chat list scroll position no longer persisted

**Priority:** High · **Upstream drift risk:** None

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt` (upstream `DisposableEffect { onDispose { … lazyListState … } }` around line 936 deleted; `lazyListState` still read around line 212 but never restored)

**Problem:**
Upstream saved and restored the chat-list scroll position across screen transitions. The fork deleted the persistence effect, so the list resets to the top every time the user returns from a chat.

**Recommended fix:**
Restore the scroll-persistence `DisposableEffect` in the SimpleUX shell.

**Acceptance criteria (DoD):**
- [ ] Returning to the chat list restores the previous scroll offset
