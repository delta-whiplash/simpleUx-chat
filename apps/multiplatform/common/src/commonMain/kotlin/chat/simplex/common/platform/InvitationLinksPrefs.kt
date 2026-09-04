package chat.simplex.common.platform

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

// SimpleUX invitation links (issue #112): one-time invitations are kept when the
// Inviter sheet closes, and the INVITE tab re-adopts the unshared one instead of
// creating another link. Adoption must never re-serve a link that already left
// the device (one-time semantics), so connIds known to have been shared are
// recorded here, locally only. Aliases live in the core (apiSetConnectionAlias),
// not here.

private const val SHARED_INVITATION_LINKS_KEY = "simpleux_invitation_links.shared"

object InvitationLinksPrefs {
  fun loadSharedIds(settings: Settings = chatFoldersSettings): Set<String> =
    decodeSharedInvitationIds(settings.getStringOrNull(SHARED_INVITATION_LINKS_KEY))

  fun markShared(connId: String, settings: Settings = chatFoldersSettings) {
    val ids = loadSharedIds(settings)
    if (connId !in ids) settings.putString(SHARED_INVITATION_LINKS_KEY, encodeSharedInvitationIds(ids + connId))
  }

  fun prune(existingConnIds: Set<String>, settings: Settings = chatFoldersSettings) {
    val ids = loadSharedIds(settings)
    val kept = ids intersect existingConnIds
    if (kept.size < ids.size) settings.putString(SHARED_INVITATION_LINKS_KEY, encodeSharedInvitationIds(kept))
  }
}

internal fun encodeSharedInvitationIds(ids: Set<String>): String =
  Json.encodeToString(SetSerializer(serializer<String>()), ids)

internal fun decodeSharedInvitationIds(raw: String?): Set<String> {
  if (raw.isNullOrEmpty()) return emptySet()
  return runCatching {
    Json.decodeFromString(SetSerializer(serializer<String>()), raw)
  }.getOrDefault(emptySet())
}
