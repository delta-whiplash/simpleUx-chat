<!-- labels: priority:low, area:hygiene, drift:none -->
# [Hygiene] Housekeeping: empty screenshot.png, README merge policy

**Priority:** Low · **Upstream drift risk:** None

**Files:**
- `apps/multiplatform/screenshot.png` (0-byte blob `e69de29` committed in `6be77cf4b`; README badges referencing it show nothing)
- `README.md` (fully rewritten for SimpleUX — will conflict on every upstream README change)

**Problem:**
An empty file was committed; the README rewrite creates a permanent, low-value conflict surface at every upstream merge.

**Recommended fix:**
Remove the empty screenshot (or replace with a real one); document in the contributing/merge guide that README intentionally diverges and should be resolved with "ours".

**Acceptance criteria (DoD):**
- [ ] No empty blobs in the tree
- [ ] README merge policy documented
