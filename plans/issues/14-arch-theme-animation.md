<!-- labels: priority:medium, area:architecture, drift:none -->
# [Architecture] ThemeAnimation: global singleton, resets user theme, hardcoded pixel origins

**Priority:** Medium · **Upstream drift risk:** None

**Files:**
- `views/ux/components/ThemeAnimation.kt:33-59`
- Origins hardcoded at `ChatListView.kt:1141` (`Offset(950f, 145f)`) and `ChatView.kt:1209` (`Offset(1000f, 145f)`)

**Problem:**
Circular-reveal origins are raw pixel values (wrong on most devices/densities). `ThemeManager.applyTheme(DARK/LIGHT)` resets the user's saved theme choice (palette overrides) on every toggle. `ThemeAnimationController` is a global mutable singleton mutated from composables.

**Recommended fix:**
Compute the reveal origin from the tapped anchor via `onGloballyPositioned`; toggle only dark/light mode while preserving the saved palette; scope the controller to the composition instead of a global singleton.

**Acceptance criteria (DoD):**
- [ ] Reveal animation starts at the tapped control on any device
- [ ] User's saved theme/palette preserved across toggles
- [ ] No global mutable controller state
