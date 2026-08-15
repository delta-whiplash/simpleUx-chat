<!-- labels: priority:medium, area:design, drift:low -->
# [Design] Typography: fix Inter alias, remove ~14 MB dead fonts, centralize styles

**Priority:** Medium · **Upstream drift risk:** Low

**Files:**
- `androidMain/.../ui/theme/Type.android.kt:6-13`, `desktopMain/.../ui/theme/Type.desktop.kt:8-15`
- `commonMain/resources/MR/fonts/` (Inter TTFs ~4.1 MB; NotoColorEmoji ~10.3 MB)
- ~75 scattered `fontSize = ` occurrences across fork views (incl. 9–11sp text)

**Problem:**
The `Inter` fontFamily actually loads **PlusJakartaSans** resources on both platforms — the commit message's claim of integrating both fonts is half false; 4.1 MB of Inter TTFs ship as dead weight. The uncommitted `EmojiFont` change to `FontFamily.Default` orphans `NotoColorEmoji-Regular.ttf` (10.3 MB) on Android (desktop still uses it). Inline `TextStyle(...)` constructions bypass Typography; half-sp sizes (16.5sp); text as small as 9sp (`ActiveContactsRail.kt:161`).

**Recommended fix:**
Load real Inter or delete the TTFs and rename the alias honestly; decide NotoColorEmoji (keep on desktop only or restore); centralize styles in `Type.kt`; floor small text ≥ 12sp (11sp for metadata maximum).

**Acceptance criteria (DoD):**
- [ ] Font alias matches what is actually loaded
- [ ] No dead font bytes shipped in APK/desktop bundles
- [ ] Text styles centralized; no sub-12sp text
