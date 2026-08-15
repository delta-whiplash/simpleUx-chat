<!-- labels: priority:high, area:drift, drift:high -->
# [Drift] ChatView.kt: revert message-grouping logic changes & unify CIContent discriminators

**Priority:** High · **Upstream drift risk:** High

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chat/ChatView.kt` (~30 hunks; `getItemSeparation` ~3711, `isSecurityOrFeatureItem` ~3704)
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chat/item/ChatItemView.kt:305-318`

**Problem:**
Beyond restyling (~30 interleaved hunks across `ChatLayout`, `ChatInfoToolbar`, `ChatItemsList`), message-grouping business logic was modified (`getItemSeparation` signature changed to take `nextItem`) and there are now **three divergent lists** of `CIContent` event types: the centering list in `ChatItemView.kt:305-318`, a second copy in `ChatView.kt` (~1902), and a different variant `isSecurityOrFeatureItem` (`ChatView.kt:3704`). Any upstream addition to the sealed `CIContent` hierarchy silently breaks centering/separation.

**Upstream impact:**
`ChatView.kt` is upstream's second most-changed screen; modified grouping logic + spread-out type discriminators guarantee both textual conflicts and silent behavioral drift.

**Recommended fix:**
Restore upstream `getItemSeparation` signature and behavior. Create ONE discriminator helper in the ux/ layer (e.g. `UxItemSemantics.isCenteredEvent(CIContent)`) consumed by all call sites. Keep toolbar/list restyling additive.

**Acceptance criteria (DoD):**
- [ ] Grouping logic matches upstream
- [ ] Single CIContent discriminator helper used everywhere
- [ ] New upstream `CIContent` types flow through automatically
