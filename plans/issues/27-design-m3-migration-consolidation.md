<!-- labels: priority:high, area:design, drift:high -->
<!-- Published: https://github.com/delta-whiplash/simpleUx-chat/issues/90 (supersedes #15, #16, #69, closed as duplicates) -->
# [Design] Migrate to Material 3 and consolidate the design system (tokens + shared components) — Android/Desktop only

**Priority:** High · **Upstream drift risk:** High (see "Drift risk" below — verify before starting)

**Scope:** `apps/multiplatform` only (Android + Desktop share this Kotlin Compose codebase). **`apps/ios` is explicitly out of scope** — iOS is deprioritized indefinitely in favor of Android, per product decision (2026-08-30). Do not port any part of this migration to Swift/SwiftUI.

## Context

This repo already tried to build a design system twice and it fragmented both times:
- `ui/theme/{Theme,Color,Shape,Type}.kt` — the original Material 2 token layer, spec-documented (`spec/services/theme.md`), mirrored to iOS.
- `ui/theme/GlassTokens.kt` + `views/ux/` — a second, parallel token layer added for the "Luxury Mineral"/glassmorphism visual direction, never reconciled with the first (#15).

Three GitHub issues already open against this exact drift are folded into this one **on creation** (superseded, not left running in parallel):
- **#16** (`design-color-tokens`) — closed once on 2026-08-15, reopened after being found *regressed*: hardcoded hex literals grew from ~150 to **470+**, and the tokens added to fix it (`Color.kt:46-52`) are referenced **0 times**.
- **#15** (`design-glass-consolidation`) — closed once, reopened after being found only partially fixed: 5 parallel glass systems reduced to "2-3", `blurRadius` param still dead.
- **#69** (`design-system tokens: half-migrated theming`) — filed 2026-08-28 as a *fresh* issue re-diagnosing the same dual-accent/five-dark-canvas/dead-theme-code problem #15 and #16 already covered, instead of reopening or referencing them. This is the fragmentation pattern happening a second time, on the tracker itself, in real time.

Related but **not** superseded (different enough scope to stay independent, but should land through the same seam this issue builds):
- **#12** (`duplication-parallel-impls`, reopened) — the button-duplication finding (50+ files rolling their own instead of `views/helpers/SimpleButton.kt`) is exactly Phase 3 below; the search-pipeline and click-dispatch duplication it also covers is unrelated to design tokens and stays out of this issue's scope.
- **#89** (`Typography phase 2`, open) — the live successor to the old #17, tracking the ~75 inline `TextStyle(...)` call sites that bypass `Type.kt`. Complementary to this issue (M3's `Typography`/`ColorScheme` is the natural landing spot for those tokens) but kept separate since it's already actively scoped.

The project's own 2026-08-28 status audit names the actual failure mode directly: *"the pattern to break is not bad code — it is closing issues and writing docs ahead of (or instead of) the code."* #69 is a live example of that same pattern recurring after the audit that named it. This issue is written to not repeat it a third time: one grep/CI-checkable Definition of Done instead of self-reported checkboxes, and #15/#16/#69 closed as duplicates of this one rather than left open to fragment further.

**Today's baseline: zero Material 3 usage anywhere in the codebase** (`grep -rl "androidx.compose.material3" apps/multiplatform` → 0 hits). Every screen and every `views/helpers/Default*` primitive is on `androidx.compose.material.*` (M2). This is a real dependency migration, not "finishing" something already started.

## Problem (three tangled issues, one root cause)

1. **No single component library is enforced.** `views/helpers/` (`SimpleButton.kt`, `DefaultDialog.kt`, `DefaultDropdownMenu.kt`, `DefaultSwitch.kt`, `Section.kt`, …) is the right seam, but ~50 view files bypass it and hand-roll their own buttons/dialogs (#12). Nothing stops a new screen from doing the same tomorrow.
2. **Colors are not tokenized in practice.** 3 incompatible accent systems, 3 near-duplicate dark backgrounds, 470+ raw hex literals in view files, and a gold accent hardcoded directly in `DefaultTopAppBar.kt:108,146` with no token at all (#16).
3. **The app sits on Material 2**, so even the "good" parts of the token layer (`Shape.kt`, `Type.kt`) are wired into an M2 `Colors`/`Shapes`/`Typography` object instead of M3's richer `ColorScheme` (which natively supports the light/dark/black/custom-palette split this app is already hand-rolling) and M3's built-in motion/elevation system (relevant to the animation/QoL goals in #14/#26).

Doing these as three separate efforts is exactly how #15/#16 already failed — a color-token pass with no enforcement regressed within two weeks. This issue treats them as one migration with one seam.

## Recommended approach (phased, seam-first)

**Phase 1 — Consolidate before converting.** Merge `GlassTokens.kt` into `ui/theme/` as the *only* color/elevation source (finishes #15); replace every raw hex literal in view files with a token reference (finishes #16); delete the dead glass files. Do this on the existing M2 types first — don't convert to M3 while there are still two color systems to convert.

**Phase 2 — Harden the seam, then swap what's behind it.** Reimplement `views/helpers/Default*.kt` and `SimpleButton*` on `androidx.compose.material3.*` primitives (`Button`, `TextField`, `AlertDialog`, `Scaffold`, `TopAppBar`, `Switch`, `DropdownMenu`, M3 `ColorScheme`/`Shapes`/`Typography`). Every caller that already goes through `views/helpers/` gets M3 for free with no per-screen edit.

**Phase 3 — Migrate the stragglers.** For each of the ~50 files that currently hand-roll a button/dialog instead of using `views/helpers/` (#12's list is the starting inventory): either migrate the call site to the shared helper, or, if a screen has a genuine one-off need, build it in `views/ux/` against the M3 primitives rather than importing `androidx.compose.material.*` directly. Add a CI grep check that fails the build on any new `import androidx.compose.material.*` (non-m3) outside an explicit legacy allowlist, so the list only shrinks.

**Suggested starting point:** `views/ux/` is already this fork's quarantine layer for new UI (per its own README) and is the newest/most idiomatic code (2026-08-28 audit: "predominantly dumb and callback-driven"). Doing the M3 conversion there first, before touching legacy upstream-derived screens like `ChatListView.kt`, is lower-risk and matches the fork's existing strategy — worth confirming as the actual sequencing before work starts.

## Drift risk — verify before starting

`ui/theme/*` and `views/helpers/*` are fork-owned and low-risk to change. But Phase 3 touches import lines in potentially every screen file, including ones that are otherwise byte-identical to upstream. **Whether that's a one-time cost or a permanent merge tax depends on whether upstream SimpleX itself is on Material 2 or Material 3** — this was not confirmed while writing this issue (no `upstream` remote was available to check). If upstream is still M2, every future upstream UI patch will conflict with this migration's import changes on every touched file, indefinitely. Confirm upstream's M3 status first; if upstream is M2 and not migrating, scope Phase 3 tightly (helpers + `views/ux/` + genuinely fork-owned screens only) rather than converting upstream-derived files wholesale.

## Caution: `ui/theme/Theme.kt` is not purely visual

The color/wallpaper model here is serialized and synced across devices (per-chat/per-user theme overrides, `.theme` file export/import — see `spec/services/theme.md`). Token renames or type changes in Phase 1/2 must preserve the existing serialization format, or theme export/import and cross-device sync will silently break even though the UI still compiles and renders fine.

## Acceptance criteria (DoD) — grep/CI-checkable, not self-reported

- [ ] `grep -rl "androidx.compose.material3" apps/multiplatform` — is the majority of the codebase (not just `views/helpers/`)
- [ ] `grep -c "0x[0-9A-Fa-f]\{6,8\}" apps/multiplatform/**/views/**/*.kt` — zero, outside `ui/theme/Color.kt`
- [ ] Zero files remaining under `ui/theme/GlassTokens.kt` / dead glass files listed in #15 — deleted, not just unreferenced
- [ ] `views/helpers/Default*`/`SimpleButton*` are M3-backed and are the only files importing `androidx.compose.material3.Button`/`AlertDialog`/etc. directly; all other files call through them
- [ ] A CI lint/grep step exists that fails on new `androidx.compose.material.*` (M2) imports outside an explicit, shrinking allowlist — so this can't regress the way #16 did
- [ ] Theme export/import (`.theme` file, `docs/THEMES.md` flow) still round-trips correctly after the token changes
- [ ] `apps/ios` has zero changes from this work

## Open questions for whoever picks this up

1. Is upstream SimpleX (Android/Desktop) itself on Material 2 or Material 3? Determines Phase 3's real drift cost (see "Drift risk" above).
2. Confirm sequencing: `views/ux/` first, legacy screens (`ChatListView.kt` et al.) opportunistically after — or is a full big-bang conversion actually wanted?

## Resolution

#15, #16, and #69 are closed as duplicates of this issue on creation (owner-confirmed 2026-08-30) — their findings are folded into "Problem" and "Phase 1" above rather than tracked in parallel.
