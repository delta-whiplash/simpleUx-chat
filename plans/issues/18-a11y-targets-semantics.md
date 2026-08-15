<!-- labels: priority:medium, area:design, drift:none -->
# [a11y] Touch targets, semantics & contrast in new components

**Priority:** Medium · **Upstream drift risk:** None

**Files:**
- `views/ux/components/SecurityBadge.kt:80-97` (~20dp clickable pill)
- `views/ux/components/FilterPillsRow.kt:103-115` (~27dp pills, no selected-state semantics)
- `views/ux/components/QuickReactionsBar.kt:76-98` (36dp emoji, 32dp plus button)
- `views/ux/components/VoiceWaveformPlayer.kt:121-149` (~20dp speed toggle, no description)
- Island tabs at `ChatListView.kt:1361` (~45dp)
- Glass text: `GlassMessageBubble`/`GlassVoiceNotePlayer` timestamps (white@0.6-0.7)

**Problem:**
Interactive targets below the 48dp Material minimum without `minimumInteractiveComponentSize`; no `Role.Tab` / `selected` semantics on the island tabs or filter pills (screen readers cannot announce active tab/filter); white-on-translucent-glass text below WCAG AA 4.5:1; light-mode SecurityBadge text ≈ 3.3:1. All custom clickables use `indication = null` (no ripple) with per-item `MutableInteractionSource`.

**Recommended fix:**
Enforce 48dp minimum interactive targets; add roles + selected/stateDescription semantics; add contentDescriptions; contrast-check on-glass text colors for both themes.

**Acceptance criteria (DoD):**
- [ ] All interactive elements ≥ 48dp
- [ ] Tabs/pills announce selected state to TalkBack
- [ ] On-glass text meets AA contrast in dark and light
