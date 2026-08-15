<!-- labels: priority:high, area:architecture, drift:low -->
# [Architecture] ux/ components must not bind the ChatModel singleton by default parameter

**Priority:** High · **Upstream drift risk:** Low

**Files:**
- `views/ux/components/FilterPillsRow.kt:43,49-51`
- `views/ux/components/SwipeableChatCard.kt:26,42,141-148`
- `views/ux/modals/QuickProfileSwitcher.kt:36`
- `views/ux/components/ProfileSwitcherOverlay.kt:229-236,388,425`
- `views/ux/components/ThemeAnimation.kt:33-59`

**Problem:**
Four components default-reference the `ChatModel` singleton (`chatModelInstance: ChatModel = ChatModel`) and the global `ntfManager`, then mutate controller state directly: `markChatRead` / `markChatUnread` / `toggleChatFavorite` (SwipeableChatCard), `chatModel.controller.changeActiveUser(...)` (ProfileSwitcherOverlay). `ThemeAnimationController` is a global mutable singleton of `mutableStateOf`/`Animatable` mutated from composables.

**Upstream impact:**
Low textually (isolated layer) — but the pattern makes components untestable, unreusable, and couples the fork's own layer to upstream model internals, defeating the purpose of the ux/ isolation strategy.

**Recommended fix:**
Explicit parameter injection from host screens. Business actions exposed as callbacks (`onMarkRead`, `onToggleFavorite`, `onSwitchProfile`, `onCancelNotifications`) implemented by a thin adapter at the host. No `= ChatModel` defaults; no global mutable controllers.

**Acceptance criteria (DoD):**
- [ ] No singleton default parameters in `views/ux/`
- [ ] Business actions flow through callbacks
- [ ] Components constructible in isolation (preview/unit test without ChatModel)
