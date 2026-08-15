<!-- labels: priority:critical, area:hygiene, drift:low -->
# [Process] Commit the untracked `views/ux/` package with its dependents as one coherent change

**Priority:** Critical · **Upstream drift risk:** Low

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/ux/**` (13 files, ~1,700 lines — untracked)
- Dependent modified files: `ChatListView.kt`, `ChatListNavLinkView.kt` (+android), `ChatItemView.kt`, `CIVoiceView.kt`, `ChatView.kt`, `HowItWorks.kt`

**Problem:**
The live SimpleUX component layer is invisible to git while the uncommitted working-tree diffs import it. Any partial commit, stash, or fresh clone breaks the build, and the entire "wave 2" of the UI overhaul is undocumented in history.

**Upstream impact:**
Untracked + uncommitted state makes rebase/merge planning impossible and risks accidental loss of ~1,700 lines of live code.

**Recommended fix:**
Commit the working-tree wave and the `ux/` tree together as a coherent "SimpleUX wave 2" commit. Add `views/ux/README.md` stating the layering rules: no `ChatModel` singleton defaults, no edits to upstream files from this layer, all strings via `MR.strings`.

**Acceptance criteria (DoD):**
- [ ] Fresh clone builds successfully
- [ ] `git status` is clean
- [ ] Commit message documents the scope; layering rules written in `views/ux/README.md`
