<!-- labels: priority:medium, area:ux, drift:low -->
# [UX] NewChatSheet: restore 1-time-link & scan entry points

**Priority:** Medium · **Upstream drift risk:** Low

**Files:**
- `views/newchat/NewChatSheet.kt` (actionButtons ~196-206, `addContact` lambda, hardcoded "Contacts" title)

**Problem:**
The "Create 1-time link" and "Scan/paste link" action buttons were deleted from the action row (only Group/Channel remain). The `addContact` lambda is still passed in but never invoked — a dead parameter. The 1-time-link flow is now only reachable via the new UserProfileView cards / chat-list header. The title is hardcoded "Contacts" instead of `MR.strings.new_chat`, and the one-hand-UI bottom bar was removed.

**Recommended fix:**
Re-expose the primary contact-connection actions (or document the new reachability clearly); remove or wire the dead `addContact` parameter; localize the title.

**Acceptance criteria (DoD):**
- [ ] 1-time-link flow reachable in ≤ 2 taps from the contacts tab
- [ ] No dead parameters
- [ ] Localized title
