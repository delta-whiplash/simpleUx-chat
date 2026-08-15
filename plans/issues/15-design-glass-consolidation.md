<!-- labels: priority:high, area:design, drift:low -->
# [Design] Consolidate to ONE token-driven, theme-aware glass surface system

**Priority:** High · **Upstream drift risk:** Low

**Files:**
- `ui/theme/GlassModifiers.kt` (live `glassSurface()`)
- `views/helpers/Glassmorphism.kt` (100% dead — `liquidGlass`, `GlassmorphicCard`, `GlassmorphicFloatingDock`, `GlassmorphicCircleButton`)
- `views/chat/glass/GlassTopAppBar.kt`, `GlassMessageBubble.kt`, `SecurityPillBadge.kt`, `GlassVoiceNotePlayer.kt` (all dead, zero call sites)
- `ui/theme/GlassTokens.kt:78-117` (dead members incl. the entire Chat-List token block and `surfaceColor()`/`borderColor()`/`blurRadius()` helpers)
- `views/ux/components/MessageBubble.kt`, `DisappearingTimerBar.kt` (dead)

**Problem:**
Five parallel glass/surface systems coexist (one live, four dead or duplicated). `GlassTokens` is a static object — not theme-aware; glass mode is dark-only by definition (`isGlassModeActive() = isInDarkTheme()`, `Theme.kt:762`). The `blurRadius` parameter of `glassSurface()` is **never used** — there is no actual blur anywhere in the new system (upstream `BlurModifier` with `RenderEffect` and the `deviceSupportsBlur` API-32 guard already exist and are reusable).

**Recommended fix:**
Keep a single `Modifier.glassSurface()` reading colors from MaterialTheme/CompositionLocal with dark and light variants; wire real blur behind the upstream `deviceSupportsBlur` guard or remove the parameter; delete every dead system.

**Acceptance criteria (DoD):**
- [ ] Single glass implementation
- [ ] Light theme renders coherently
- [ ] Blur parameter either functional (with API guard) or removed
- [ ] Dead glass files deleted
