<!-- labels: priority:critical, area:trust, drift:high -->
# [Trust] Remove fabricated "public directory" injected into chat search

**Priority:** Critical · **Upstream drift risk:** High

**Files:**
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt:2194-2269` (`sampleDirectoryGroups`, `PublicDirectorySearchResultsSection`)
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/newchat/NewChatSheet.kt`

**Problem:**
Five hardcoded fake groups ("Annuaire SimpleX (Groupes publics)") with invented member counts ("1,500+ membres", "Groupe officiel SimpleX Chat", three sharing garbled group IDs) are injected into live search results and join on tap via `connectIfOpenedViaUri` (`ChatListView.kt:1617`).

**Upstream impact:**
Fabricated server data presented as real in a privacy-focused messenger is a trust & safety hazard. It is also embedded in the fork's highest-drift file, compounding merge conflicts.

**Recommended fix:**
Delete the section entirely. If a directory feature is desired later, build it as a real feature in the `views/ux/` layer — never hardcoded sample data presented as server results.

**Acceptance criteria (DoD):**
- [ ] No fabricated results in search
- [ ] No connect action triggered from sample data
- [ ] `sampleDirectoryGroups` removed from the codebase
