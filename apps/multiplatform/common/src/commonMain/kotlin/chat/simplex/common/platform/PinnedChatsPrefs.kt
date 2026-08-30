package chat.simplex.common.platform

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// SimpleUX pin ("pin to top") persistence for chats (FB-14), backed by its own
// moko multiplatform-settings instance — same pattern as StarredChatsPrefs, and
// kept separate from AppPreferences for the same reason (model/SimpleXAPI.kt is
// byte-frozen; the settings store must not be touched during ChatModel class
// init, so ids are seeded via ChatModel.loadPersistedPinnedChats() from the
// platform app init instead). Pinning is a local display preference only: it is
// never sent to contacts or the core.
expect val pinnedChatsSettings: Settings

private const val PINNED_CHAT_IDS_KEY = "simpleux.pinned.chatIds"

object PinnedChatsPrefs {
  fun loadPinnedChatIds(settings: Settings = pinnedChatsSettings): Set<String> =
    decodePinnedChatIds(settings.getStringOrNull(PINNED_CHAT_IDS_KEY))

  fun savePinnedChatIds(ids: Collection<String>, settings: Settings = pinnedChatsSettings) {
    settings.putString(PINNED_CHAT_IDS_KEY, encodePinnedChatIds(ids))
  }
}

// Encoding is JSON so chat ids never collide with a separator character.
internal fun encodePinnedChatIds(ids: Collection<String>): String =
  Json.encodeToString(ListSerializer(String.serializer()), ids.toList())

internal fun decodePinnedChatIds(raw: String?): Set<String> {
  if (raw.isNullOrEmpty()) return emptySet()
  return runCatching {
    Json.decodeFromString(ListSerializer(String.serializer()), raw).toSet()
  }.getOrDefault(emptySet())
}
