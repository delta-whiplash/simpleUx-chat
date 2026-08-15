<!-- labels: priority:medium, area:trust, drift:none -->
# [Trust] ServerRadarSheet shows simulated relay diagnostics

**Priority:** Medium · **Upstream drift risk:** None

**Files:**
- `views/ux/modals/ServerRadarSheet.kt` (wired at `ChatListView.kt:1108` with `isConnected = chatModel.chatRunning.value == true`)

**Problem:**
The sheet displays fabricated diagnostics — "SimpleX SMP v2", "100% Unidirectionnelle" — and uses controller-running as a proxy for servers-reachable, which is not equivalent. Invented metrics presented as real status undermine trust in genuine security indicators.

**Recommended fix:**
Wire real SMP server statuses from the controller (upstream exposes server connection state), or clearly label the content as illustrative.

**Acceptance criteria (DoD):**
- [ ] No invented metrics displayed as real status
- [ ] Connectivity reflects actual server reachability or is labeled illustrative
