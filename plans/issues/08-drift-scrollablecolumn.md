<!-- labels: priority:medium, area:drift, drift:high -->
# [Drift] ScrollableColumn.android.kt: stop unconditionally rewriting expect/actual layout

**Priority:** Medium · **Upstream drift risk:** High

**Files:**
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/platform/ScrollableColumn.android.kt` (+31 lines uncommitted)

**Problem:**
Both `ColumnWithScrollBar` and `ColumnWithScrollBarNoAppBar` had their `oneHandUI` conditional layout branches deleted and now unconditionally apply the bottom-bar layout + `NavigationBarBackground`. This is a platform expect/actual file used by every settings/detail screen on Android.

**Upstream impact:**
Platform expect/actual files churn upstream; unconditional branch deletion here conflicts on every upstream touch and affects screens far beyond the chat list.

**Recommended fix:**
Keep upstream branches intact. Gate the SimpleUX layout decision via a flag/CompositionLocal read at the call site (or in the shell), leaving this file with a minimal or zero diff.

**Acceptance criteria (DoD):**
- [ ] File diff vs upstream minimal or zero
- [ ] SimpleUX layout unaffected
- [ ] Upstream one-hand behavior still functional when pref enabled
