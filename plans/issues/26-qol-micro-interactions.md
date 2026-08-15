<!-- labels: priority:low, area:ux, drift:low -->
# [QoL] Positive micro-interaction backlog (frontend-only, isolated in views/ux/)

**Priority:** Low · **Upstream drift risk:** Low

**Context:**
Suggested QoL enhancements grounded in building blocks the fork already has (`SwipeableChatCard`, `FilterPillsRow`, `QuickReactionsBar`, quote context in message items). Each must ship inside the `views/ux/` layer with zero edits to upstream files.

**Suggestions:**
1. **Swipe-to-reply** — extend `SwipeableChatCard`'s gesture model to chat items; compose the reply quote into `SendMsgView` on release.
2. **Jump-to-message** — make quote/context chips in `ChatItemView` scroll the message list to the referenced item (upstream has scrollTo logic to reuse).
3. **Saved filters** — persist `FilterPillsRow` selection (unread/groups/contacts) across sessions.
4. **Scroll-to-unread divider** — "new messages" separator with tap-to-jump (list state already tracks last read).
5. **Desktop shortcuts** — Ctrl+K focus search, Ctrl+F in-chat find, Esc close modals (desktop-only expect/actual).
6. **Per-chat media gallery** — grid view of images/videos/files from the chat info screen.
7. **Grouped timestamps** — show time separators on message groups rather than per item.

**Acceptance criteria (DoD):**
- [ ] Each feature implemented solely in `views/ux/` + host wiring
- [ ] No upstream file edits required
- [ ] Localized + a11y-compliant from the start (see issues 18/19)
