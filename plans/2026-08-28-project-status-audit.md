# SimpleUX Project Status Audit — 2026-08-28

**Scope:** project state vs ambition, GitHub tracker honesty, code quality of the fork delta, live bug root causes, upstream drift, work organization (milestones).
**Method:** three parallel evidence-gathering passes (closure verification against code, post-audit commit review, ambition/git-hygiene sweep), re-measured on live remotes after `git fetch origin && git fetch upstream`. Every claim below carries `file:line` or commit evidence. Companion to the 2026-08-15 audit (`2026-08-15-ux-audit-backlog.md`).
**Base:** `HEAD = ecc329e28` on `stable` at audit start.

---

## 1. Executive summary

**Is it shitty code? No — but the process around it is failing, and that is now the bigger risk.**

- The non-negotiable tenets hold: **zero wire/protocol/crypto changes** (committed model-layer delta vs upstream is ~20 lines of UI state in `ChatModel.kt`), and the three trust hazards found on 2026-08-15 are genuinely repaired in code.
- `views/ux/` is a real, mostly parameterized component layer, under version control since `83ab1db21`.
- But: **13 issues were closed without the matching code existing**, the README claims features that don't exist, ~5 commits' worth of delivered work sat unpushed on one machine, a second line of work diverged onto an unmerged side branch, and the fork's biggest structural debt (the chat-list rewrite inside upstream's highest-churn file) **grew** while its fix sat on that side branch.
- Upstream moved fast during August (**64 commits ahead on master**, v7.1.0-beta.1); the fork's merge surface is still manageable but concentrated in exactly the files upstream churns most.

The pattern to break is not bad code — it is **closing issues and writing docs ahead of (or instead of) the code**.

## 2. Fork topology & git state (facts)

| Item | Value |
|---|---|
| Fork base | `abd595467` (merge-base with upstream, 2026-08-12 era) |
| Committed delta `abd595467..HEAD` | 176 files, +10,909 / −2,457 |
| Top churn files | `ChatListView.kt` (1,379 lines changed), `UserProfileView.kt` (610), `ChatView.kt` (513), `ChatItemView.kt` (397), `WhatsNewView.kt` (373) |
| Unpushed local commits (Aug 20) | `c13d3997a` (feature drop: directory, starred chats, animations, join bar), `2b8645516`, `dc2248961`, `ecc329e28` (white-bar fixes) |
| Uncommitted "wave 3" | 8 files, +75/−27 — **re-introduces model-layer preference edits** (see §6.4) |
| Branches | `stable` (tip), stale `auto/issue-2-…` (merged via PR #27), misleading `feature/haptic-feedback` (= `c13d3997a`, carries far more than haptics) |
| Remote state at audit time | `origin/stable = e785d3ac9`; **plus one previously-unknown branch, see §7** |

**The only committed model-layer delta is `ChatModel.kt` (+20 lines, `starredChatIds`/`starredMessageIds` — in-memory only). `SimpleXAPI.kt` committed delta vs upstream is empty.** The wave-3 working tree temporarily breaks that record (§6.4).

## 3. Upstream drift (live, 2026-08-28, after fetch)

| Metric | Value |
|---|---|
| Behind `upstream/stable` | **8 commits** (`a99541d0f`…`083fb0a70`; v7.0.1 released) |
| Behind `upstream/master` | **64 commits** (was 22 on Aug 15; v7.1.0-beta.1) |
| File overlap of the 8 stable commits with fork-touched files | 3, all peripheral: `WhatsNewView.kt`, `WhatsNewView.swift`, `gradle.properties` → **near-term stable merge is cheap** |
| Notable upstream move | `5e45fe1f0` "directory: only create group links after approval" — upstream is building real directory infrastructure, relevant to SimpleUX's directory feature |

The master gap (64) is where 7.1 core bumps live; planning a master alignment separately is advised. The blocking risk remains internal: §6.1.

## 4. Tracker honesty audit

### 4.1 The 2026-08-15 audit issues (#1–#26), verified against code on 2026-08-28

| Verdict | Issues |
|---|---|
| **Fixed (verified)** | #2 (real PQ badge, `SecurityBadgeState.kt:20-33`), #3 (`views/ux/` tracked, 17 files), #11 (scroll persistence restored, `ChatListView.kt:216-221`), #20 (onboarding routing conditional, `HowItWorks.kt:29-34`), #21 (1-time link & scan wired, `NewChatSheet.kt:196`), #25 (empty screenshot.png gone) |
| **Partially fixed** | #1 (fake `sampleDirectoryGroups` gone → real fetch of `directory.simplex.chat`; residual hardcoded "Directory Bot" entry, French fallback labels), #5 (`oneHandUI` restored at HEAD — but wave-3 restarts the model-edit pattern), #13 (compact/error variants restored; speed toggle & waveform still fake), #15 (5 glass systems → 2-3; `blurRadius` still an unused parameter, `GlassModifiers.kt:29`), #19 (`app_name` = "SimpleUX" everywhere; still 7 hardcoded literals in ux/, French strings outside ux/), #22 (localized + disclaimer added; still `isConnected = chatRunning`, no real server status) |
| **Not fixed (closed anyway)** | #6 (`getItemSeparation` still 3-param diverged, `ChatView.kt:3765`; 2+ discriminator lists), #7 (UserProfileView still a 610-line rewrite, no ModalView/IME options), #8 (ScrollableColumn.android.kt still unconditionally rewritten), #10 (stale `remember(cInfo.id)`, `ChatView.kt:1533`), #12 (dead `ChatListSearchBar` ~110 lines + dead `VoiceLayout`; 3 click-dispatch paths), #14 (ThemeAnimation singleton + pixel origins unchanged), #16 (regressed: 470+ hex literals, was ~150; new tokens in `Color.kt:46-52` referenced **0 times**), #17 (Inter alias still loads PlusJakartaSans; ~14 MB dead fonts) |
| **Open** | #4 (the merge blocker — see §7), #9 (zero `= ChatModel` defaults left in ux/ — good; `ProfileSwitcherOverlay.kt:233` direct controller call and `ThemeAnimationController` singleton remain) |

### 4.2 The wishlist batch #28–#45 (created AND closed 2026-08-18)

| Class | Issues | Evidence |
|---|---|---|
| Implemented before/at closure | #31, #33, #39, #41 (+ #35 mostly — island tab icons excluded) | code present at closure date |
| Implemented 2 days AFTER closure, in unpushed `c13d3997a` | #28, #30, #32, #38, #42, #40 (partial: stars in-memory only, lost on restart) | added in commit dated Aug 20; closed Aug 18 |
| Partial / misattributed | #34 ("swipe-to-reply" — actually swipe = mark-read/favorite; no reply gesture exists anywhere), #43 (no sticker code; generic bubble pop only), #44 (avatar restyle only, nothing animated) | grep evidence |
| **False closure claims** | #29 (no double-tap gesture in codebase), #36 (`LiveTypingWave` doesn't exist; typing dots are untouched upstream code; referenced `GlassTopAppBar.kt` was deleted), #37 ("documented" — no doc exists), #45 (no streak code at all) | grep evidence |

Also: #24 closed with a **false claim** — "debug applicationIdSuffix defaults to .debug" — while `apps/multiplatform/build.gradle.kts:24` defaults the suffix to `""` (debug collides with release). Its other two items are real (schemes added, FLAG_SECURE unconditional).

### 4.3 README vs code

Honest: screenshot/GIF assets all exist (`docs/images/`, sizes sane); filter pills, island bar, gold accent, real PQ badge, `chat.simplex.ux` applicationId + provider authority all verified in code.
Not backed by code: "swipe-to-reply" (×2, doesn't exist), `chat.simplex.ux.debug` suffix (defaults to `""`), "Tests 100% Passing" badge (no fork CI on `stable` — the branch in §7 adds one, unmerged).

## 5. What is genuinely good (keep)

1. **Protocol tenet intact** — the only reason this fork is shippable at all.
2. Trust-critical repairs are real: no fabricated sample data presented as server results (replaced by a live, cached, IO-dispatched fetch in `SimpleUxDirectoryRepository.kt`); SecurityBadge derives tri-state from `connPQEnabled`.
3. `views/ux/` components are predominantly "dumb" and callback-driven now (#9's main goal met).
4. Onboarding, scroll persistence, NewChatSheet entry points — three real UX regressions from wave 1 were actually fixed.
5. `InfoRow`/`DefaultTopAppBar` reworks are functional improvements; README media is real.
6. The 2026-08-22 side branch (§7) shows the right instincts: CI first, extraction second.

## 6. Live bugs — root causes (evidence)

### 6.1 White bar on Android chat list (light theme) — still open; the two "fix" commits missed
- `2b8645516` removed a **desktop-only** divider (`ChatListNavLinkView.desktop.kt`); `dc2248961` removed a divider inside the **chat view** group-link card (`CIChatLinkHeader.kt:55`). Neither renders on the Android chat list.
- **Leading suspect (2026-08-20 emulator forensics, fixed in `ecc329e28`, visual confirmation pending):** the captcha image sent by group admins in the pending-join flow rendered full-width inside chat-list rows (unbounded `CIImageView` smallView + unbounded `SmallContentPreview` in `ChatPreviewView.kt`); its white/cream stripes read as the bar, matching the then-reported symptom pattern (all themes, mid-screen, pending-group rows).
- Residual candidates for light-theme reports **after** the captcha fix is visually confirmed:
  1. `SwipeableChatCard.kt:120` — row background flips to opaque `Color(0xFFFFFFFF)` in light theme as soon as `abs(offsetX) > 1f`; any diagonal scroll micro-drag triggers it (every row is wrapped, `ChatListNavLinkView.kt:79`).
  2. `ChatListNavLinkView.android.kt:32` — rows painted pure white on the Carrara background `0xFFF8FAFC` (`Theme.kt:691`); the removed border was dark translucent, never the white element.
  3. `values/themes.xml:7-9` — white `windowBackground` follows system (not app) theme; `androidSetNightModeIfSupported` is a no-op stub (`Platform.kt:24`) → white bands during transitions.
- The uncommitted wave-3 border removal in `ChatListNavLinkView.android.kt` removes a *dark* hairline — cosmetic change, not the white-bar fix.

### 6.2 Desktop island bar never renders — root cause confirmed
`UI.desktop.kt:18` hardcodes `getKeyboardState() = KeyboardState.Opened` (desktop has no IME); the bar renders only when `keyboardState == KeyboardState.Closed` (`ChatListView.kt:263`). Same broken predicate zeroes `bottomPadding` (`:245,255`).

### 6.3 Theme stuck "mid-gray" — two distinct mechanisms
- **Transient wedge — ThemeAnimation overlay leak.** `ThemeAnimation.kt`: `isAnimating.value = true` set before launch (`:44`), reset only after the 650 ms reveal (`:62`); call sites pass screen-scoped scopes (`ChatListView.kt:1131`, `ChatView.kt:1184`) — any cancellation (back navigation) leaves the permanently-hosted overlay (`zIndex 9999f`) drawing a **frozen partial circle** over the app. The palette is applied mid-animation at `delay(180)` (`:54-60`), so a cancel in between leaves overlay color ≠ actual theme. Secondary: double-trigger race (no `isAnimating` guard; stale `currentlyDark`).
- **Persistent variant — NOT the overlay.** The 2026-08-20 emulator session measured the mid-gray surviving `am force-stop` + restart (mean pixel ~95-115 vs ~25-40 dark / ~230+ light) — impossible for runtime-only state. Primary suspect there: the glass-mode base layer (`isGlassModeActive() == isInDarkTheme()`, `Theme.kt:762`; `AmbientGlassBackground` draws its own base) or palette/splash mismatch. Both tracks are recorded on reopened #14.

### 6.4 Wave-3 (uncommitted) defects to fix BEFORE committing
1. **Model-layer violation**: `SimpleXAPI.kt` flips `chatItemRoundness` 0.75→1 and `chatItemTail` true→false (defaults in `AppPreferences` + `AppSettings`) — the exact anti-pattern issue #5 closed.
2. **Dead migration**: `AppCommon.kt:61-65` gates on `lastMigration < 366` while `gradle.properties:28` ships `android.version_code=366` — existing 366 users never run it; only ≤365 upgraders do. Needs version bump 367 + `< 367`.
3. Stray `// LALAL VERSION CODE` comment (`AppCommon.kt:45`); duplicated hex pairs in `DefaultTopAppBar.kt:71-74`; dead conditional `widthIn(max = if (…) 220.dp else 220.dp)` (`SimpleXInfo.kt:111`).
4. `MR/base/strings.xml` branding edit desyncs 10 existing translations that still say "SimpleX".

## 7. The "lost" Aug-22 work — found on a side branch

Issue #4's comments referenced commits `6d2c2e7` / `3ca7108`, a `simpleux.yml` CI and `plans/2026-08-22-recentrage-projet.md` — none present in this clone or on `origin/stable` (reflog spans Aug 14→20 with no Aug-22 activity; origin/stable tip was `e785d3ac9`). Initially assessed as lost; **`git fetch origin` then revealed branch `origin/claude/code-ambitions-analysis-0opfll` containing all of it, plus more:**

| Commit | Content |
|---|---|
| `6d2c2e776` | fork CI (`simpleux.yml`, 70 lines), `views/ux/README.md` layering doc, fabricated-data removal in ChatListView (−219), `oneHandUI` → `true` (identical fix to ours — merge-clean), recentrage doc (+84) |
| `3ca7108f5` | **shell extraction (#4)**: `views/ux/ChatListShell.kt` (+520), ChatListView −460 (2127 → ~1670 lines; still ~525 lines diff vs upstream, target <120) |
| `5c8649374` | build-verification notes for the extraction |
| `3df6abc91`, `61f8a76fd` | on-demand Android APK workflow + CI fix |
| `7ff3d17d3` | central quick-camera button with always-on QR auto-detect (`views/ux/camera/`) |

Branch delta: 14 files, +1,268/−704, diverged from `e785d3ac9`. Our local line had 4 unpushed commits since the same base. **Overlap: 4 files** — `SimpleXAPI.kt` (identical change, auto-merges), `NewChatSheet.kt`, `MR/base/strings.xml`, and **`ChatListView.kt` (both sides rewrote it — real conflict work)**. Dead `ChatListSearchBar` survives on the branch, so #12's reopen stands post-merge.

Decision recorded by maintainer 2026-08-28: finish audit/tracker/milestones first, then merge the branch into `stable` in this session. **Merge executed same day** (see §10).

## 8. Ambition vs state — has the project drifted?

**The technical mission is intact; the epistemic process drifted.**

1. **Frontend-only, coexistence, protocol-compat: holding.** Android identity ~90% real (`chat.simplex.ux`, provider, new `simplex-ux`/`simpleux` schemes; debug-suffix gap remains). iOS (AGENTS.md §3.2): **zero work** — bundle ids still `chat.simplex.app` (#23 correctly open).
2. **Tension markers**: #48–#51 [Perf/Native] (Rust/C++ SIMD, zero-copy IPC replacing the JNI boundary, GHC RTS) directly contradict AGENTS.md §1.2's "never alter the FFI layer" — parked under `scope:deferred`, they should either be explicitly re-scoped as a future "SimpleUX Native" track or closed.
3. **The real drift is bookkeeping**: 13 closures without code, README ahead of reality, progress claimed in tracker comments pointing at unreachable commits, delivered features unpushed. On 2026-08-28 the tracker said X, the branches said Y, and only this audit reconciled them.
4. **Structural debt grew while its fix existed unmerged**: ChatListView.kt's fork delta rose to +1,163/−216 (25 hunks) on the local line while the extraction sat on the side branch.

## 9. Work organization (milestones created on GitHub)

| Milestone | Issues |
|---|---|
| **M1 — Drift & merge-blockers** | #4 (merge branch, then finish extraction to <120-line diff), #6, #7, #8, #9, #12 |
| **M2 — Correctness & live bugs** | #10, #13, #14, + new issues filed today (white bar, desktop island bar, wave-3 defects, dead swipe/star wiring, README accuracy) |
| **M3 — Design system & i18n** | #15, #16, #17, #19 |
| **M4 — Identity & platform QoL** | #23, #24, #46, #47 |
| **M5 — Icebox (native/deferred)** | #48, #49, #50, #51 |

**Sequencing rationale:** M1 unblocks every future upstream merge (8 behind stable, 64 behind master — each week this grows). M2 contains user-visible lies and wedges. M3 is what makes the "design system" claim in the README true. M4/M5 are deliberate, scheduled non-priorities.

## 10. Tracker corrections applied (2026-08-28)

- **Reopened with evidence comments** (closed without matching code, verified 2026-08-28): #6, #7, #8, #10, #12, #14, #15, #16, #17, #24, #29, #36, #37, #45.
- **Evidence comments, no reopen**: #1 (directory bot residual), #4 (side-branch location of Aug-22 work; merge scheduled), #9 (updated state: defaults gone, controller calls remain), #13 (speed/waveform still fake), #19 (residual literals + 5-locale coverage), #22 (still no real server wiring).
- **New issues filed**: Android chat-list white bar (root-cause candidates), desktop island bar (confirmed cause), wave-3 pre-commit defects (migration collision + model-layer revert), dead swipe/star wiring + in-memory stars, README accuracy.
- All (re)opened issues assigned to the milestones in §9.

## 11. Merge executed (2026-08-28, same day)

`claude/code-ambitions-analysis-0opfll` merged into `stable` as `03c5779df` after conflict resolution:

- **ChatListView.kt** (the real conflict): kept the branch's shell extraction (inline `TelegramTopHeader` definition deleted — it lives in `views/ux/ChatListShell.kt` now, including all of our search wiring: `nameSearchCandidate`, debounce, `connectNameCandidate`, verified symbol-by-symbol before dropping the inline copy); kept **our** keyboard-gate on the island bar (hides it while searching — the branch had removed the gate entirely; the desktop fix is tracked as #59 at the `UI.desktop.kt` level, which is the correct layer); kept **our** real `PublicDirectorySearchResultsSection` (branch's fabricated-data removal intent is subsumed); wired the branch's `onOpenCamera` quick-camera parameter into the gated call site. One spurious brace from indentation-divergent lambda alignment fixed by hand.
- **NewChatSheet.kt**: kept our directory-search section (branch side was a deletion of pre-existing junk).
- **strings.xml**: kept our `server_radar_*` strings including the `illustrative_notice` key referenced by `ServerRadarSheet.kt:116-120`.
- `SimpleXAPI.kt`: auto-merged (both lines made the identical `oneHandUI → true` fix).

Result: ChatListView.kt at 2,074 lines; **fork diff vs upstream base for that file down from +1,163/−216 to +767/−223** (still ~5.4× the <120-line target — the tab host, filter pills and search state remain interleaved, see #4). Total fork delta now 185 files, +11,744/−2,464. The wave-3 working tree was stashed and restored untouched.

**Validation:** `:common:compileKotlinDesktop` and `:common:compileDebugKotlinAndroid` both BUILD SUCCESSFUL (the latter covers `androidMain`, incl. the branch's QuickCamera actuals; only pre-existing deprecation warnings). Runtime verification on the emulator is still recommended before pushing, especially: island bar behavior while searching, quick-camera button on Android, directory search join flow, light-theme chat list (white bar #58).
