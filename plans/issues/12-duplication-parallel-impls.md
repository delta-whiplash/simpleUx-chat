<!-- labels: priority:high, area:architecture, drift:low -->
# [Duplication] Eliminate parallel implementations (search pipeline, row clicks, voice row)

**Priority:** High · **Upstream drift risk:** Low

**Files:**
- `ChatListView.kt:817-926` (dead `ChatListSearchBar`) vs `1193-1233` (live near-copy inside `TelegramTopHeader` — includes `apiConnectPlan` + debounce logic; the two copies have already diverged)
- `views/chat/item/CIVoiceView.kt:122` (dead `VoiceLayout`, ~140 lines)
- `views/chatlist/ChatListNavLinkView.kt:66-82` (outer `defaultClickAction` duplicates the per-branch click logic that still exists inside each `when` branch)

**Problem:**
Duplicated business logic has already diverged between copies (the dead search copy has a `focusRequester.requestFocus()` branch the live one lacks). Row-click dispatch now depends on `SwipeableChatCard`'s `abs(offsetX) < 5f` gate interacting with nested clickables. Dead upstream-derived `VoiceLayout` coexists with the live `VoiceWaveformPlayer`.

**Upstream impact:**
Diverged copies silently miss upstream fixes; dead code inflates the fork's maintenance surface.

**Recommended fix:**
One live search pipeline (wire `TelegramTopHeader` to a shared search ViewModel or restore the upstream composable); one click-dispatch path in `ChatListNavLinkView`; delete dead copies.

**Acceptance criteria (DoD):**
- [ ] One search pipeline with connect/debounce logic
- [ ] One click-dispatch path
- [ ] Dead duplicates deleted
