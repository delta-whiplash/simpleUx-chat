<!-- labels: priority:high, area:architecture, drift:none -->
# [Correctness] SecurityBadge displays stale chat state (remember keyed on id only)

**Priority:** High · **Upstream drift risk:** None

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chat/ChatView.kt:1530`

**Problem:**
```kotlin
val currentChat = remember(cInfo.id) { chatModel.chats.value.firstOrNull { it.chatInfo.id == cInfo.id } }
```
Keyed only on `cInfo.id`, which never changes for a given chat — the lookup never re-evaluates when `chatModel.chats` updates (contact verification, member changes, security upgrades). The `SecurityBadge` shows stale data for the lifetime of the composable.

**Recommended fix:**
Derive from state directly (no `remember`) or key on the chats snapshot so recomposition follows `chatModel.chats` updates.

**Acceptance criteria (DoD):**
- [ ] Badge updates when chat verification/member/security state changes
