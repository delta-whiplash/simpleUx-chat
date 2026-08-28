# Instructions for AI agents (and humans) — SimpleUX Chat

Read this file in full before touching anything. It overrides your general
intuitions about "good git hygiene", "how to close an issue", or "how to
write idiomatic Compose" — these rules are specific to this project and its
history. **Every rule cites the incident that created it. All of them have
already been violated and paid for.** Full context:
`plans/2026-08-28-project-status-audit.md`.

## 0. What this project is

SimpleUX is a **frontend-only** fork of simplex-chat: same protocol, same
Haskell core, same network behavior, radically better UX. "Uncompromising
privacy meets world-class user experience." It must coexist side-by-side
with the official SimpleX app on the same device. The Haskell core
(`libsimplex`/`libapp`) and JNI/FFI bridge are the stable, authoritative
backend layer — our playground is the frontend: Kotlin Multiplatform /
Compose (`apps/multiplatform/`) today, iOS later.

## 1. Non-negotiable invariants

- **Frontend-only. Never touch the backend.** No changes to Haskell code,
  JNI/FFI signatures (`Core.kt`), wire formats, serialization, or crypto.
  100% interoperability with official SimpleX clients is the reason this
  fork may exist. An issue that seems to require native/Rust/C++/GHC-RTS
  work (#48–#51) lives in milestone **M5 Icebox** — escalate, don't
  implement.
- **The model layer is byte-frozen.** `model/SimpleXAPI.kt` must remain
  byte-identical to upstream. UI preference *defaults* are changed in the
  UI layer, never by flipping `AppPreferences`/`AppSettings` defaults.
  → Violated twice: issue #5 (oneHandUI flip, cost a full remediation),
  then again 6 days later in wave-3 (chatItemRoundness/chatItemTail, #60).
  If you find yourself editing SimpleXAPI.kt for cosmetics, the approach
  is wrong.
- **Coexistence identity is fixed:** applicationId `chat.simplex.ux`,
  provider authority `chat.simplex.ux.provider`, `simplex-ux`/`simpleux`
  schemes. Never reintroduce anything that collides with
  `chat.simplex.app`.

## 2. Before starting any task

1. **Check the tracker first**
   (`gh issue list -R delta-whiplash/simpleUx-chat`): is an issue already
   covering what you're about to do? Is another session already on it?
   Comment/assign before working. Two sessions worked this tracker
   simultaneously on 2026-08-28 and one silently re-closed an issue the
   other had reopened with evidence (#13).
2. **One session = one checkout.** Run `git status` before and during
   work. If the tree is dirty with a wave you didn't create, or another
   agent is active on this clone, **stop and coordinate instead of
   layering work**.
3. **`gh` commands ALWAYS carry `-R delta-whiplash/simpleUx-chat`.**
   Without it, gh resolves to **upstream** `simplex-chat/simplex-chat` —
   on 2026-08-28 this posted a comment on an unrelated upstream issue
   (deleted within the minute, but still). Repo-less `gh api` calls must
   use full `repos/delta-whiplash/...` paths too.
4. **Scope comes from milestones M1–M6** (on GitHub). Don't invent new
   scope; file an issue and let the human triage it.

## 3. Truth rules (tracker, commits, docs)

This is the project's biggest failure class. The tracker must say what the
code says.

- **Close an issue only with the fixing commit hash + a one-line code
  verification (file:line).** "Implemented" without a hash is a false
  closure. On 2026-08-18, fourteen issues were closed without the matching
  code existing anywhere (#6, #7, #8, #10, #12, #14, #15, #16, #17, #24,
  #29, #36, #37, #45) — several had literally no code, ever.
- **Re-closing an issue after an evidence-based reopen requires a commit
  link.** A silent re-close gets reopened, with an escalating note (#13).
- **README claims must be grep-able in code.** No feature, suffix, badge,
  or benchmark documented that a search can't verify. The README advertised
  "swipe-to-reply" (no such gesture exists), a `.debug` suffix (defaults
  to `""`), and "Tests 100% Passing" (no fork CI existed) — #62.
- **Commit messages are scope-honest.** `feat: complete i18n localization
  across all languages` that touched 5 of ~40 locales is a lie in the git
  log forever (e785d3ac9). State the exact scope: files, locales, screens.
- **Never reference artifacts that don't exist.** No citing commits, CI
  files, or plan docs you haven't verified. Issue #4 cited commit
  `3ca7108` while that commit was unreachable from any remote — broken
  work nearly counted as "done" because a comment said so.

## 4. Git & session process

- **Working tree ends every session clean-ish.** Commit coherent work, or
  stash with a descriptive message, or explicitly hand off in the session
  summary. A "wave" of uncommitted edits that silently crosses days is how
  trust incidents happen — wave-3 sat 8 days, including model-layer edits
  (#60).
- **All session work lands on `stable`, or on a branch merged within the
  same session.** No branch holds unique work past the session that
  created it. The `claude/code-ambitions-analysis-0opfll` branch diverged
  for 6 days and its existence was only discovered by accident — unmerged
  work is lost work.
- **Push only when the human asks.** Local `stable` is the integration
  line; origin is updated deliberately.
- **Migration discipline:** any gate on `lastMigration < N` requires
  bumping `android.versionCode` to a value **greater than N**. A migration
  gated `< 366` while versionCode is already 366 never runs for exactly
  the users it targets (#60). Also: no stray debug comments in committed
  code (`// LALAL VERSION CODE` was about to ship in wave-3).
- **One commit = one coherent topic.** No mixing a feature drop, a config
  change, and three unrelated bugfixes in one commit (c13d3997a carries
  six features).

## 5. Code rules

- **`views/ux/` is the SimpleUX layer.** New composables live there:
  parameterized, side-effect-free, actions via callbacks, **no
  `= ChatModel` default parameters, no global mutable singletons** (#9).
  Components must be constructible in isolation (preview/test).
- **Extraction, not rewrite.** Upstream's high-churn files carry a diff
  budget: `ChatListView.kt` target < ~120 lines vs upstream (it sat at
  +1,163 across 25 hunks — every upstream touch becomes a conflict, #4).
  Keep uncalled upstream composables in place as merge buffer instead of
  deleting them opportunistically.
- **One accent system. Tokens only.** No new raw `0xFF…` literals in view
  code — use `ui/theme/Color.kt` tokens + `MaterialTheme`. The token file
  existed with **zero usages** while views accumulated 470+ literals and 4
  accent families (#16). Gold is `AmberGold`, not a new hex per file.
- **No hardcoded user-visible strings.** Everything through `MR.strings`
  (English base + French). French literals in code are bugs regardless of
  how they look (#19). New MR keys must not desync existing locale files.
- **No fake affordances.** Every visible control must be wired to real
  behavior. Speed toggles that don't change playback (#13) or relay panels
  that present fixed labels as live status (#22) are trust bugs, not UX
  debt. If content is illustrative, it must say so on screen.
- **Delete dead code; don't accumulate it.** No unreferenced copies kept
  "for later" (#12: dead `ChatListSearchBar`, dead `VoiceLayout`, four
  uncalled upstream composables). No unreferenced resource bytes — 14 MB
  of dead fonts plus an `Inter` alias that actually loads PlusJakartaSans
  shipped for weeks (#17).
- **Design language is Luxury Mineral** — follow
  `.agents/skills/simpleux-design-system/SKILL.md`. Don't invent a second
  glass/surface system; consolidate into the existing one (#15).

## 6. Validation gates (before claiming anything works)

- **Kotlin compile is the floor.**
  `JAVA_HOME=/c/Users/Delta/jdk-21/jdk-21.0.5+11 ./gradlew :common:compileKotlinDesktop`
  must be green before any commit. For `androidMain` changes:
  `:common:compileDebugKotlinAndroid`. (cmake fails in Git Bash — don't
  chase full APK builds from the CLI.)
- **Fix the bug where it lives.** Before declaring a bug fixed, reproduce
  it — or at minimum verify the fix touches the platform/file where it
  reproduces. Two commits "fixed" the chat-list white bar by removing a
  desktop-only divider and a chat-view card divider; the Android list
  never rendered either (#58).
- **UI work gets eyes on it.** The human runs an emulator live
  (emulator-5554) — coordinate before capturing. No "fixed" claim without
  either a reproduction match or his visual confirmation.
- **A bug stays open until the fix is confirmed on-device**, even when the
  commit looks right. Interim findings and candidate root causes go in
  issue comments, not closure claims.
- **Three of these rules are CI-enforced** by the `truth-check` job in
  `.github/workflows/simpleux.yml` (model layer frozen, migration gates
  reachable, no new hardcoded French literals). If it goes red, the rule
  won — fix the code, not the check.

## 7. Out of scope — do not improvise

If a task seems to require one of these, stop and raise it instead:

- Native/runtime changes of any kind (Rust, C++, SIMD, GHC RTS, IPC
  bridges) — M5 Icebox, explicitly deferred
- Gamification wishlist items (streaks, animated wallpapers, scheduled
  messages…) — they exist as issues; implementation needs a human go
- iOS identity work (bundle id, app groups) — starts with M4 and requires
  macOS/Xcode anyway
- New themes, new glass systems, new accent palettes — #15/#16 exist to
  *reduce* this surface, not grow it

## 8. When unsure

Comment on the GitHub issue (English; always with `-R`!) rather than
guessing and implementing a broad interpretation. The human reads comments
regularly and answers in French. Asking is cheaper than a reverted feature.

## Quick reference

| Thing | Value |
|---|---|
| Repo / tracker | `delta-whiplash/simpleUx-chat` (fork of `simplex-chat/simplex-chat`) |
| Integration branch | `stable` (commit directly; push on request) |
| Main code | `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/` |
| SimpleUX layer | `.../views/ux/` (see its `README.md` for layering rules) |
| Roadmap | GitHub milestones M1–M6 |
| Design system | `.agents/skills/simpleux-design-system/SKILL.md` |
| Latest full audit | `plans/2026-08-28-project-status-audit.md` |
| Build checks | `:common:compileKotlinDesktop` / `:common:compileDebugKotlinAndroid` (JDK 21, see §6) |
| Rules CI | `simpleux.yml` → `truth-check` job (runs on every push + PR) |
| Android identity | `chat.simplex.ux` · provider `chat.simplex.ux.provider` |
