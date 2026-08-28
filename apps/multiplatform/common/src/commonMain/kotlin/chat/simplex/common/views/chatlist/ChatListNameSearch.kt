package chat.simplex.common.views.chatlist

import chat.simplex.common.model.*
import chat.simplex.common.platform.chatModel

// Extracted verbatim from ChatListView.kt (issue #4): pure name-search helpers of the
// connect-by-name pipeline. Same package, so existing imports (ChatListShell.kt,
// NewChatSheet.kt) keep working.

// Default top-level part used to complete a bare name typed in the search field (search field only;
// the message parser and the wire format are unchanged).
private const val DEFAULT_NAME_TLD = "testing"
// Shortest name that offers the button, so it is discoverable but does not flash on short prefixes.
private const val MIN_NAME_LENGTH = 5
// Wait this long after the last keystroke before the local name search runs.
internal const val NAME_SEARCH_DEBOUNCE_MS = 300L

private val nameLabelRegex = Regex("[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*")
private fun isNameLabel(s: String): Boolean = s.length in 1..63 && nameLabelRegex.matches(s)

// On-device candidate for connecting by SimpleX name: the string sent to the core to resolve it.
// Mirrors the domain grammar (nameLabelP/mkDomain in SimplexName.hs): an optional @/# prefix, then
// dot-separated ASCII labels; a dotless word is completed with the default top-level part. Returns
// the string to send (keeping @/# so the type is preserved), or null when the text is not a name.
internal fun nameSearchCandidate(str: String): String? {
  val text = str.trim()
  val prefix = text.firstOrNull()?.takeIf { it == '@' || it == '#' }
  val core = if (prefix != null) text.substring(1) else text
  val labels = core.split(".")
  if (core.isEmpty() || labels.any { !isNameLabel(it) }) return null
  return when {
    labels.size > 1 -> text                                            // already has a top-level part
    core.length >= MIN_NAME_LENGTH -> "${prefix ?: ""}$core.$DEFAULT_NAME_TLD"
    else -> null
  }
}

// The chat id a local (PRMNever) search resolved to — a contact, a business, or a channel — or null on a miss.
// The core returns the correct type for @ vs # (getContactToConnect / type-filtered getGroupToConnect), so no
// client-side type check is needed.
internal suspend fun knownChatId(rhId: Long?, result: ConnectionPlanResult?): String? = when (val plan = result?.connectionPlan) {
  is ConnectionPlan.ContactAddress -> (plan.contactAddressPlan as? ContactAddressPlan.Known)?.contact?.let { contact ->
    // a name-resolved chat may be prepared in the store but not yet listed, so add it (as the tap path does)
    if (chatModel.getContactChat(contact.contactId) == null) {
      chatModel.chatsContext.addChat(Chat(remoteHostId = rhId, chatInfo = ChatInfo.Direct(contact), chatItems = emptyList()))
    }
    contact.id
  }
  is ConnectionPlan.GroupLink -> (when (val g = plan.groupLinkPlan) {
    is GroupLinkPlan.Known -> g.groupInfo
    is GroupLinkPlan.OwnLink -> g.groupInfo
    else -> null
  })?.let { gInfo ->
    if (chatModel.getGroupChat(gInfo.groupId) == null) {
      chatModel.chatsContext.addChat(Chat(remoteHostId = rhId, chatInfo = ChatInfo.Group(gInfo, groupChatScope = null), chatItems = emptyList()))
    }
    gInfo.id
  }
  else -> null
}
