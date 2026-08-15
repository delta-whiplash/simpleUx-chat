<!-- labels: priority:critical, area:trust, drift:low -->
# [Trust] SecurityBadge hardcodes `isPQ = true` (fabricated post-quantum indicator)

**Priority:** Critical · **Upstream drift risk:** Low

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/ux/components/SecurityBadge.kt:37-78`

**Problem:**
`val isPQ = true` is hardcoded, so the badge claims "PQ Chiffré / Kyber / ML-KEM actif" for every direct chat regardless of the actual connection state. Upstream `E2EEInfo.pqEnabled` is tri-state and negotiated per connection. The component imports `Connection` but never reads it.

**Upstream impact:**
None textually (file lives in the isolated ux/ layer) — but misrepresenting encryption status in a privacy messenger is a severe correctness/trust defect.

**Recommended fix:**
Derive PQ status from the chat's real `E2EEInfo`/connection state; render tri-state (PQ / standard E2EE / unencrypted); localize strings via `MR.strings`.

**Acceptance criteria (DoD):**
- [ ] Badge reflects the real `pqEnabled` state of the connection
- [ ] Tri-state rendering implemented
- [ ] All badge strings localized via MR.strings
