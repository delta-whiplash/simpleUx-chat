<!-- labels: priority:high, area:design, drift:low -->
# [Design] Tokenize colors: 150+ hardcoded hex, 3 accent palettes, 3 darks, light splash flash

**Priority:** High · **Upstream drift risk:** Low

**Files:**
- `views/chatlist/ChatListView.kt` (61 literals), `views/chat/item/ChatItemView.kt` (~15), `views/chatlist/ChatPreviewView.kt` (10), plus ~40 in `views/ux/**`
- `views/helpers/DefaultTopAppBar.kt:107-112,150` (untokenized gold accent)
- `apps/multiplatform/android/src/main/res/values/themes.xml` + `values-night/themes.xml` (raw hex, light splash dark)
- `ui/theme/Theme.kt:667,687`

**Problem:**
Three incompatible accent systems coexist: telegram blue (dark `0xFF2AABEE` ≠ light `0xFF0088CC` — two different brand blues), GlassTokens blue/cyan/violet, and gold (`0xFFE2B755`/`0xFFD97706`) present in no token file. Three near-identical dark backgrounds: `#0A0E17` (themes.xml) vs `0xFF0E121B` (Theme.kt background) vs `0xFF07090E` (GlassTokens). The light-mode splash screen uses a dark background (`#0F172A`) while the window background is white → visible dark→white flash on cold start.

**Recommended fix:**
Define the accent once in `Color.kt`/palette; replace all view-level literals with theme tokens; align the dark backgrounds across layers; give light mode a light splash.

**Acceptance criteria (DoD):**
- [ ] Zero raw hex colors in view files (grep-based lint)
- [ ] One accent system, consistent dark/light
- [ ] Single dark background constant across layers
- [ ] Clean light-mode cold start (no flash)
