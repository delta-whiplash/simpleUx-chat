# SimpleUX Frontend & Upstream-Drift Audit — 2026-08-15

**Scope:** UI/UX layer, upstream synchronization risk, frontend architecture, design system, UX/QoL.
**Method:** full diff analysis of the fork against upstream base `abd595467` (committed + uncommitted + untracked changes), cross-checked with in-source verification of the most severe findings.
**Deliverable:** this document + the GitHub issue backlog in [`plans/issues/`](issues/) (created via `create-issues.sh`).

---

## 1. Fork topology (facts)

| Layer | Extent | Notes |
|---|---|---|
| Committed | `6be77cf4b` — 49 files, +2,864/−1,154 | "Telegram layout & glassmorphic aesthetics" overhaul |
| Uncommitted | 20 files, +1,475/−507 | Second wave: tab shell, UserProfileView rewrite, onboarding swap |
| Untracked | `views/ux/**` — 13 files, ~1,700 lines | Live components **not in git at all** |

- No `upstream` remote was configured before this audit (only `origin` → fork). Added during the audit: `https://github.com/simplex-chat/simplex-chat`.
- **No protocol / wire / crypto changes anywhere.** The only model-layer diff is one preference default (`oneHandUI`, `SimpleXAPI.kt:269`).
- Strategy observed: **"rewrite in place"**, not "add alongside" — concentrated in upstream's highest-churn files.

## 2. Upstream drift baseline

_Computed after `git fetch upstream` (see section appended at the bottom with live numbers)._

## 3. Verified critical findings

1. **Fabricated public directory** — `ChatListView.kt:2194-2269`: `sampleDirectoryGroups` (5 fake groups, invented member counts, "Groupe officiel SimpleX Chat") injected into live search results; tapping joins via `connectIfOpenedViaUri`.
2. **Fabricated security indicator** — `views/ux/components/SecurityBadge.kt:37`: `val isPQ = true` hardcoded; every direct chat shows "PQ Chiffré / Kyber / ML-KEM actif" regardless of the real negotiated connection state.
3. **Live code outside version control** — the whole `views/ux/` package is untracked while working-tree diffs import it; any partial commit/stash/clone breaks the build.

## 4. Issue backlog (26 issues)

Labels: `priority:{critical|high|medium|low}` · `area:{trust|drift|architecture|design|ux|identity|hygiene}` · `drift:{blocking|high|low|none}`.
Individual issue files live in [`plans/issues/`](issues/) and are ready to be created with [`create-issues.sh`](issues/create-issues.sh).

---

### [Trust] Remove fabricated "public directory" injected into chat search — `01`

- **Priority:** Critical · **Drift risk:** High · **Labels:** `priority:critical` `area:trust` `drift:high`
- **Files:** `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt:2194-2269`, `views/newchat/NewChatSheet.kt`
- **Problem:** Five hardcoded fake groups ("Annuaire SimpleX (Groupes publics)") with invented member counts ("1,500+ membres", "Groupe officiel SimpleX Chat") appear in live search results and join on tap via `connectIfOpenedViaUri` (`ChatListView.kt:1617`).
- **Upstream impact:** Fabricated server data in a privacy messenger is a trust & safety hazard — and it is embedded in the highest-drift file, so it also complicages future merges.
- **Recommended fix:** Delete the section entirely. If a directory feature is desired later, implement it as a real feature in the `views/ux/` layer, never as hardcoded sample data presented as server results.
- **DoD:** no fabricated results in search; no connect action from sample data; `sampleDirectoryGroups` gone from the codebase.

### [Trust] SecurityBadge hardcodes `isPQ = true` — `02`

- **Priority:** Critical · **Drift risk:** Low · **Labels:** `priority:critical` `area:trust` `drift:low`
- **Files:** `views/ux/components/SecurityBadge.kt:37-78`
- **Problem:** The badge claims "PQ Chiffré / Kyber / ML-KEM actif" for every direct chat regardless of actual connection state. Upstream `E2EEInfo.pqEnabled` is tri-state and negotiated per connection.
- **Recommended fix:** Derive PQ status from the chat's real `E2EEInfo`/connection; tri-state rendering (PQ / standard E2EE / unencrypted); localize via `MR.strings`.
- **DoD:** badge reflects real `pqEnabled`; strings localized.

### [Process] Commit the untracked `views/ux/` package with its dependents — `03`

- **Priority:** Critical · **Drift risk:** Low · **Labels:** `priority:critical` `area:hygiene` `drift:low`
- **Files:** `views/ux/**` (13 files, ~1,700 lines) + dependent modified files (ChatListView, ChatListNavLinkView, ChatItemView, CIVoiceView, ChatView, HowItWorks).
- **Problem:** Live SimpleUX components are invisible to git; the uncommitted diffs import them — any partial commit, stash or fresh clone breaks the build, and "wave 2" is undocumented.
- **Recommended fix:** Commit the working-tree wave + `ux/` tree together as a coherent "SimpleUX wave 2" commit; add a `views/ux/README.md` stating the layering rules (no ChatModel singleton defaults, no upstream file edits).
- **DoD:** fresh clone builds; `git status` clean; scope documented in the commit message.

### [Drift] Extract the SimpleUX chat-list shell out of ChatListView.kt — `04`

- **Priority:** High · **Drift risk:** **Blocking** · **Labels:** `priority:high` `area:drift` `drift:blocking`
- **Files:** `views/chatlist/ChatListView.kt` (+644 committed, +794 uncommitted)
- **Problem:** In-place rewrite of upstream's highest-churn screen: 17+ interleaved hunks; upstream features deleted in place (`ToggleChatListCard`, `ChatListFeatureCards`, `NewChatSheetFloatingButton`, `oneHandUI` branches, scroll-persistence effect); hosts a tab-navigation architecture (`SimpleUxTab` CHATS/CONTACTS/SETTINGS/PROFILE) replacing upstream's modal model. The uncommitted round already re-rewrites the committed round (`TelegramTopHeader` grew from 4 to 9 params).
- **Upstream impact:** Nearly every upstream touch to this file will conflict — this is the fork's merge blocker.
- **Recommended fix:** Move the SimpleUX shell (`TelegramTopHeader`, `TelegramBottomIslandBar`, `IslandTabItem`, `SimpleUxTab` host, `ActiveContactsRail` wiring) to `views/ux/ChatListShell.kt`; reduce the `ChatListView.kt` diff to a minimal scaffold swap at one call site; **keep unused upstream composables in place** (dead-but-intact upstream code is a merge buffer).
- **DoD:** `ChatListView.kt` diff vs upstream < ~120 lines; shell lives in `views/ux/`; behavior unchanged; deleted upstream feature calls restored or consciously documented.

### [Drift] Revert model-layer edit: oneHandUI default flipped in SimpleXAPI.kt — `05`

- **Priority:** High · **Drift risk:** High · **Labels:** `priority:high` `area:drift` `drift:high`
- **Files:** `model/SimpleXAPI.kt:269`; `views/usersettings/Appearance.android.kt:108`; stranded reads: `views/TerminalView.kt:78`, `views/chatlist/TagListView.android.kt:55`, `views/chat/item/ChatItemInfoView.kt:513`, `views/chat/ChatView.kt:1001`
- **Problem:** The fork's only model-layer change flips a shared preference default (`oneHandUI` true→false) while the setting UI was removed from Appearance and 4 files still read it — a half-applied removal with semantic ripple across platforms.
- **Recommended fix:** Restore the upstream default; the SimpleUX shell ignores the pref at its single scaffold site. If one-hand UI must die, finish the removal in the UI layer only.
- **DoD:** `SimpleXAPI.kt` byte-identical to upstream; no stranded half-behavior reads.

### [Drift] ChatView.kt: revert grouping logic changes & unify CIContent discriminators — `06`

- **Priority:** High · **Drift risk:** High · **Labels:** `priority:high` `area:drift` `drift:high`
- **Files:** `views/chat/ChatView.kt` (~30 hunks; `getItemSeparation` ~3711, `isSecurityOrFeatureItem` ~3704), `views/chat/item/ChatItemView.kt:305-318`
- **Problem:** Beyond restyling, message-grouping business logic was modified (`getItemSeparation` signature now takes `nextItem`) and there are now **three divergent lists** of `CIContent` event types (ChatItemView centering list, a second copy in ChatView ~1902, and `isSecurityOrFeatureItem`). Any upstream addition to the sealed hierarchy silently breaks centering/separation.
- **Recommended fix:** Restore upstream `getItemSeparation`; create ONE discriminator helper in the ux/ layer consumed by all call sites; keep toolbar restyling additive.
- **DoD:** grouping logic matches upstream; single discriminator helper.

### [Drift] UserProfileView.kt: reduce rewrite surface & restore lost behaviors — `07`

- **Priority:** Medium · **Drift risk:** High · **Labels:** `priority:medium` `area:drift` `drift:high`
- **Files:** `views/usersettings/UserProfileView.kt` (+685 uncommitted)
- **Problem:** Full in-place rewrite of the profile editor. Functional parity mostly kept (pick/crop/resize/save, unsaved-changes guard — verified) but lost: the privacy explainer copy, `ModalView` desktop/modal sizing, one-hand support, `ProfileNameField` IME/KeyboardOptions. Mixed FR/EN hardcoded copy.
- **Recommended fix:** Wrap instead of replace (restyle on upstream structure); restore ModalView + explainer + keyboard options; localize.
- **DoD:** diff is restyle-only; desktop modal sizing works; no mixed-language strings.

### [Drift] ScrollableColumn.android.kt: stop unconditionally rewriting expect/actual layout — `08`

- **Priority:** Medium · **Drift risk:** High · **Labels:** `priority:medium` `area:drift` `drift:high`
- **Files:** `androidMain/.../platform/ScrollableColumn.android.kt` (+31 uncommitted)
- **Problem:** `oneHandUI` conditional branches deleted unconditionally in a platform expect/actual file upstream actively maintains (settings/detail screens) — every upstream touch conflicts.
- **Recommended fix:** Keep upstream branches; gate the SimpleUX layout via a flag/CompositionLocal at the call site.
- **DoD:** file diff minimal or zero; SimpleUX layout unaffected.

### [Architecture] ux/ components must not bind the ChatModel singleton — `09`

- **Priority:** High · **Drift risk:** Low · **Labels:** `priority:high` `area:architecture` `drift:low`
- **Files:** `views/ux/components/FilterPillsRow.kt:43`, `SwipeableChatCard.kt:26,42,141-148`, `modals/QuickProfileSwitcher.kt:36`, `components/ProfileSwitcherOverlay.kt:229-236`, `components/ThemeAnimation.kt:33-59`
- **Problem:** Four components default-reference `ChatModel` / global `ntfManager` (`chatModelInstance: ChatModel = ChatModel`) and mutate controller state directly (`markChatRead`/`markChatUnread`/`toggleChatFavorite`/`changeActiveUser`); `ThemeAnimationController` is a global mutable singleton. Components are not testable or reusable, and business actions live in the view layer.
- **Recommended fix:** Explicit parameter injection from host screens; business actions exposed as callbacks (`onMarkRead`, `onToggleFavorite`, `onSwitchProfile`) implemented by a thin adapter; no `= ChatModel` defaults, no global mutable controllers.
- **DoD:** no singleton defaults in ux/; actions flow through callbacks; components constructible in isolation (preview/test).

### [Correctness] SecurityBadge displays stale chat state — `10`

- **Priority:** High · **Drift risk:** None · **Labels:** `priority:high` `area:architecture` `drift:none`
- **Files:** `views/chat/ChatView.kt:1530`
- **Problem:** `remember(cInfo.id) { chatModel.chats.value.firstOrNull { ... } }` — keyed only on `cInfo.id`, which never changes for a given chat, so the lookup never re-evaluates when the chats list updates (verification, member changes). The badge shows stale data.
- **Recommended fix:** Derive from state directly (no remember) or key on the chats snapshot.
- **DoD:** badge updates on chat verification/member changes.

### [Regression] Chat list scroll position no longer persisted — `11`

- **Priority:** High · **Drift risk:** None · **Labels:** `priority:high` `area:ux` `drift:none`
- **Files:** `views/chatlist/ChatListView.kt` (upstream `DisposableEffect` scroll persistence ~936 deleted; `lazyListState` read ~212 but never restored)
- **Recommended fix:** Restore scroll persistence in the SimpleUX shell.
- **DoD:** returning to the list restores scroll offset.

### [Duplication] Eliminate parallel implementations — `12`

- **Priority:** High · **Drift risk:** Low · **Labels:** `priority:high` `area:architecture` `drift:low`
- **Files:** `ChatListView.kt:817-926` (dead `ChatListSearchBar`) vs `1193-1233` (live copy inside `TelegramTopHeader`, already diverged — includes `apiConnectPlan` + debounce logic); `CIVoiceView.kt:122` (dead `VoiceLayout`, ~140 lines); `ChatListNavLinkView.kt:66-82` (outer `defaultClickAction` duplicates per-branch click logic)
- **Problem:** Duplicated business logic has already diverged between copies; row-click dispatch depends on `SwipeableChatCard`'s `abs(offsetX) < 5f` gate interacting with nested clickables.
- **Recommended fix:** One live search pipeline (wire `TelegramTopHeader` to a shared search ViewModel or restore the upstream composable); one click-dispatch path; delete dead copies.
- **DoD:** one search pipeline; one click-dispatch path; dead copies deleted.

### [Regression] Voice notes: fake speed control, fake waveform, lost layouts — `13`

- **Priority:** High · **Drift risk:** Low · **Labels:** `priority:high` `area:ux` `drift:low`
- **Files:** `views/ux/components/VoiceWaveformPlayer.kt:35,134-138`, `views/chat/item/CIVoiceView.kt:92-104`
- **Problem:** The speed toggle cycles local state but never applies to `AudioPlayer`; waveform bars are static fake data; `smallView` (chat-list preview now renders a full 38dp interactive player), `hasText` compact layout and the `brokenAudio` error indicator were lost vs upstream `VoiceLayout`. The dead `GlassVoiceNotePlayer` import remains at `CIVoiceView.kt:26`.
- **Recommended fix:** Apply speed to `AudioPlayer` (upstream supports it); real waveform or an honest placeholder; restore compact variants + error state; remove dead import.
- **DoD:** speed actually changes playback; compact/error variants restored.

### [Architecture] ThemeAnimation: global singleton, resets user theme, pixel origins — `14`

- **Priority:** Medium · **Drift risk:** None · **Labels:** `priority:medium` `area:architecture` `drift:none`
- **Files:** `views/ux/components/ThemeAnimation.kt:33-59`; origins hardcoded at `ChatListView.kt:1141` (`Offset(950f, 145f)`) and `ChatView.kt:1209` (`Offset(1000f, 145f)`)
- **Problem:** Circular-reveal origins are raw pixels (wrong on most devices); `ThemeManager.applyTheme(DARK/LIGHT)` resets the user's saved theme choice on every toggle; the controller is global mutable state mutated from composables.
- **Recommended fix:** Compute the reveal origin from the tapped anchor via `onGloballyPositioned`; toggle only dark/light preserving the saved palette; scope the controller to the composition.
- **DoD:** reveal starts at the tapped control on any device; user palette preserved.

### [Design] Consolidate to ONE token-driven, theme-aware glass system — `15`

- **Priority:** High · **Drift risk:** Low · **Labels:** `priority:high` `area:design` `drift:low`
- **Files:** `ui/theme/GlassModifiers.kt` (live), `views/helpers/Glassmorphism.kt` (100% dead), `views/chat/glass/*` (3 of 5 dead), `ui/theme/GlassTokens.kt:78-117` (dead members incl. the whole Chat-List token block)
- **Problem:** Five parallel glass/surface systems. `GlassTokens` is a static object (not theme-aware); glass is dark-only by definition (`isGlassModeActive() = isInDarkTheme()`, `Theme.kt:762`); the `blurRadius` parameter of `glassSurface()` is **never used** — there is no real blur (upstream `BlurModifier` + `deviceSupportsBlur` API-32 guard are reusable).
- **Recommended fix:** Keep one `Modifier.glassSurface()` reading colors from MaterialTheme/CompositionLocal (dark + light variants); wire real blur behind the upstream guard or drop the parameter; delete the other systems.
- **DoD:** single glass implementation; light theme coherent; blur real or parameter removed.

### [Design] Tokenize colors: 150+ hex literals, 3 accents, 3 darks, splash flash — `16`

- **Priority:** High · **Drift risk:** Low · **Labels:** `priority:high` `area:design` `drift:low`
- **Files:** `ChatListView.kt` (61 literals), `ChatItemView.kt` (~15), `ChatPreviewView.kt` (10), `DefaultTopAppBar.kt:107-112,150` (untokenized gold), `themes.xml` + `values-night/themes.xml`, `Theme.kt:667,687`
- **Problem:** Three incompatible accent systems coexist (telegram blue with dark `0xFF2AABEE` ≠ light `0xFF0088CC`; GlassTokens blue/cyan/violet; gold `0xFFE2B755`/`0xFFD97706` in no token file). Three near-identical dark backgrounds (`#0A0E17` / `0xFF0E121B` / `0xFF07090E`). Light-mode splash is dark (`#0F172A`) → visible dark→white flash on cold start.
- **Recommended fix:** Define the accent once in `Color.kt`/palette; replace literals with theme tokens; align backgrounds; light splash uses the light color.
- **DoD:** zero raw hex in view files (grep lint); one accent system; clean light-mode cold start.

### [Design] Typography: fix Inter alias, remove ~14 MB dead fonts — `17`

- **Priority:** Medium · **Drift risk:** Low · **Labels:** `priority:medium` `area:design` `drift:low`
- **Files:** `androidMain/.../ui/theme/Type.android.kt:6-13`, `desktopMain/.../Type.desktop.kt:8-15`, `resources/MR/fonts/`, ~75 scattered `fontSize` (incl. 9–11sp)
- **Problem:** The `Inter` fontFamily actually loads **PlusJakartaSans** resources (the commit claim "Integrate Plus Jakarta Sans & Inter" is half false); 4.1 MB of Inter TTFs are dead weight; `NotoColorEmoji` (10.3 MB) orphaned on Android by the `EmojiFont` change; inline `TextStyle` constructions bypass Typography; half-sp sizes (16.5sp).
- **Recommended fix:** Load real Inter or delete the TTFs and rename the alias; decide NotoColorEmoji; centralize styles in `Type.kt`; floor small text ≥ 12sp.
- **DoD:** honest alias; no dead font bytes shipped; styles centralized.

### [a11y] Touch targets, semantics & contrast in new components — `18`

- **Priority:** Medium · **Drift risk:** None · **Labels:** `priority:medium` `area:design` `drift:none`
- **Files:** `SecurityBadge.kt:80-97` (~20dp clickable), `FilterPillsRow.kt:103-115` (~27dp pills, no selected state), `QuickReactionsBar.kt:76-98` (32dp plus button), `VoiceWaveformPlayer.kt:121-149` (~20dp speed toggle, no description), island tabs `ChatListView.kt:1361`
- **Problem:** Interactive targets below 48dp without `minimumInteractiveComponentSize`; no `Role.Tab` / `selected` semantics on tabs and pills; white-on-translucent glass text below 4.5:1 (light-mode SecurityBadge ≈ 3.3:1).
- **Recommended fix:** 48dp minimum targets, roles + state semantics, contentDescriptions, contrast-checked on-glass text colors.
- **DoD:** interactive elements ≥ 48dp; tabs/pills announce state; AA contrast on glass.

### [UX/i18n] Restore MR.strings localization; fix localized app_name — `19`

- **Priority:** High · **Drift risk:** Low · **Labels:** `priority:high` `area:ux` `drift:low`
- **Files:** 26+ literals in `views/ux/**`, `ChatItemView.kt:743`, `ChatListView.kt:1601`, `UserProfileView.kt` (mixed FR/EN); `MR/{fr,de,es,fi,ja,...}/strings.xml` (`app_name` still "SimpleX")
- **Problem:** Dozens of hardcoded French strings bypass the MR i18n system entirely; localized launcher labels show "SimpleX" instead of "SimpleUX" on French/German/etc. devices.
- **Recommended fix:** Extract all literals to `MR.strings` (base EN + FR translations); remove stale localized `app_name` overrides.
- **DoD:** zero hardcoded user-visible strings in fork code; launcher label SimpleUX in all locales.

### [UX] Onboarding: routing broken for existing users & desktop paths lost — `20`

- **Priority:** High · **Drift risk:** Low · **Labels:** `priority:high` `area:ux` `drift:low`
- **Files:** `views/onboarding/HowItWorks.kt:30`, `views/ux/modals/ZeroJargonOnboarding.kt`
- **Problem:** Hardcoded `onboardingStage.set(Step2_CreateProfile)` routes **existing users** back into profile creation (upstream routed `user != null → OnboardingComplete`); desktop loses the LinkAMobile + DB-passphrase branches; the new onboarding copy is French-only.
- **Recommended fix:** Restore conditional routing on user state; keep localized copy or provide ZeroJargonOnboarding in all locales via MR.
- **DoD:** existing user completes onboarding; desktop branches intact; localized.

### [UX] NewChatSheet: restore 1-time-link & scan entry points — `21`

- **Priority:** Medium · **Drift risk:** Low · **Labels:** `priority:medium` `area:ux` `drift:low`
- **Files:** `views/newchat/NewChatSheet.kt` (actionButtons 196-206, dead `addContact` param, hardcoded "Contacts" title)
- **Problem:** "Create 1-time link" and "Scan/paste link" buttons were deleted (the flow is now only reachable via UserProfile cards / chat-list header); the `addContact` lambda is still passed but never invoked — dead parameter.
- **Recommended fix:** Re-expose the actions or document the new reachability; remove/wire the dead param; localize the title (`MR.strings.new_chat`).
- **DoD:** 1-time-link reachable in ≤ 2 taps; no dead params.

### [Trust] ServerRadarSheet shows simulated relay diagnostics — `22`

- **Priority:** Medium · **Drift risk:** None · **Labels:** `priority:medium` `area:trust` `drift:none`
- **Files:** `views/ux/modals/ServerRadarSheet.kt` (wired with `isConnected = chatRunning` at `ChatListView.kt:1108`)
- **Problem:** "SimpleX SMP v2" and "100% Unidirectionnelle" are fabricated; controller-running is not servers-reachable.
- **Recommended fix:** Wire real SMP server statuses from the controller, or clearly label the content as illustrative.
- **DoD:** no invented metrics displayed as real.

### [Identity] iOS coexistence not started — `23`

- **Priority:** Medium · **Drift risk:** None · **Labels:** `priority:medium` `area:identity` `drift:none`
- **Files:** `apps/ios/**` (`project.pbxproj` `PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.app`, Info.plist)
- **Problem:** AGENTS.md §3.2 requires bundle id `chat.simplex.ux` + app groups `group.chat.simplex.ux`; iOS is entirely untouched by the fork.
- **Recommended fix:** Rename bundle id, app groups, display name SimpleUX (requires macOS/Xcode).
- **DoD:** side-by-side install with the official app verified.

### [Identity] Android gaps: deep-link collision, debug suffix, FLAG_SECURE — `24`

- **Priority:** Medium · **Drift risk:** None · **Labels:** `priority:medium` `area:identity` `drift:none`
- **Files:** `AndroidManifest.xml:71-109`, `apps/multiplatform/build.gradle.kts:25`, `MainActivity.kt:49`
- **Problem:** The `simplex` scheme + `autoVerify` App Links on `simplex.chat`/`smp*.simplex.im` are identical to the official app → side-by-side installs fight over links (contrary to AGENTS.md §3.1); the debug application-id suffix defaults to `""` (debug collides with release); `FLAG_SECURE` is silently skipped in all debug builds (`privacyProtectScreen && !BuildConfig.DEBUG`).
- **Recommended fix:** Decide the link strategy (chooser-friendly or dedicated handling per AGENTS); default the debug suffix to `.debug`; gate FLAG_SECURE behind an explicit developer opt-out.
- **DoD:** documented link behavior; debug installs alongside release; FLAG_SECURE honored unless explicitly disabled.

### [Hygiene] Housekeeping: empty screenshot.png, README merge policy — `25`

- **Priority:** Low · **Drift risk:** None · **Labels:** `priority:low` `area:hygiene` `drift:none`
- **Files:** `apps/multiplatform/screenshot.png` (0-byte blob `e69de29` committed in `6be77cf4b`), `README.md` (full fork rewrite)
- **Recommended fix:** Remove the empty file (or fill it); document that README intentionally diverges (accept divergence at merge time).
- **DoD:** no empty blobs in the tree; documented merge policy.

### [QoL] Positive micro-interaction backlog (frontend-only) — `26`

- **Priority:** Low · **Drift risk:** Low · **Labels:** `priority:low` `area:ux` `drift:low`
- **Suggestions (grounded in existing fork building blocks):** extend `SwipeableChatCard` to swipe-to-reply with quote chips; jump-to-message from quote context; wire `FilterPillsRow` to saved filters; scroll-to-unread divider; desktop keyboard shortcuts (Ctrl+K search focus); per-chat media gallery; grouped timestamps display.
- **DoD:** each feature ships inside `views/ux/` with zero edits to upstream files.

---

## 5. What is already good (keep it)

- **No wire/protocol/crypto changes** — the tenet holds; `SimpleXAPI.kt` is one preference default.
- `filteredChats()` / `filtered()` business logic untouched; no `ChatModel`/`ChatController` API misuse beyond upstream's own patterns.
- The `views/ux/` layer is the **right architectural move** — most components are properly parameterized "dumb" components.
- `SettingsView.kt` is an additive restyle (low merge risk) — a model to follow.
- Android identity ~90 % compliant (`chat.simplex.ux`, provider authority, base name).
- No debug logging / TODO litter added by the fork.

## 6. Recommended sequencing

1. **Before anything:** issues `01`, `02`, `03` (trust + git coherence) and `05` (model-layer revert).
2. **Before the next upstream merge:** `04` (shell extraction — unblocks all future rebases), then `06`, `08`.
3. **Steady state:** design-system consolidation (`15`–`18`), i18n (`19`), remaining UX regressions (`07`, `11`, `13`, `20`, `21`).
4. **Opportunistic:** identity (`23`, `24`), QoL (`26`).

---

## 7. Upstream drift baseline (live numbers, 2026-08-15)

Remote `upstream` → `https://github.com/simplex-chat/simplex-chat` (added by this audit). Fork point = `abd595467` (merge-base with both `upstream/stable` and `upstream/master`).

| Metric | Value |
|---|---|
| Ahead of `upstream/stable` | **1 commit** (`6be77cf4b`: 49 files, +2,864/−1,154) |
| Behind `upstream/stable` | **0 commits** (stable has not moved since the fork) |
| Behind `upstream/master` | **22 commits** (83 files, +1,973/−1,130) |
| Uncommitted wave | 20 files, +1,475/−507 |
| Untracked | `views/ux/**`, 13 files, ~1,700 lines |

**File overlap between upstream/master's 22 commits and ALL fork-touched files (committed + uncommitted + untracked): exactly ONE file — `model/SimpleXAPI.kt`.**
Upstream modified `SMPAgentError` (~line 8072: new `A_LINK` error, `A_PROHIBITED` now carries a payload) while the fork flipped a preference default at line 269 — different regions, auto-mergeable today.

**Interpretation:** the next rebase onto `upstream/master` is currently near-conflict-free. This is the moment to fix the structural issues (issues `03`, `04`, `05`) — every in-place rewrite retained makes the following merge progressively more expensive. The merge-conflict forecasts in this backlog are forward-looking (based on how upstream historically churns `ChatListView.kt`, `ChatView.kt` and the platform files), not a measure of today's textual overlap.
