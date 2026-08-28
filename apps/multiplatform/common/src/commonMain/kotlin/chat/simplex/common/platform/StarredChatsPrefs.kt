package chat.simplex.common.platform

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// SimpleUX local star ("favorite") persistence for chats, backed by its own
// moko multiplatform-settings instance. Kept separate from AppPreferences
// (model/SimpleXAPI.kt is byte-frozen) so the star state can be seeded into
// ChatModel.starredChatIds at startup and written through on toggle.
expect val starredChatsSettings: Settings

private const val STARRED_CHAT_IDS_KEY = "simpleux.starred.chatIds"

object StarredChatsPrefs {
  fun loadStarredChatIds(settings: Settings = starredChatsSettings): Set<String> =
    decodeStarredChatIds(settings.getStringOrNull(STARRED_CHAT_IDS_KEY))

  fun saveStarredChatIds(ids: Collection<String>, settings: Settings = starredChatsSettings) {
    settings.putString(STARRED_CHAT_IDS_KEY, encodeStarredChatIds(ids))
  }
}

// Encoding is JSON so chat ids never collide with a separator character.
internal fun encodeStarredChatIds(ids: Collection<String>): String =
  Json.encodeToString(ListSerializer(String.serializer()), ids.toList())

internal fun decodeStarredChatIds(raw: String?): Set<String> {
  if (raw.isNullOrEmpty()) return emptySet()
  return runCatching {
    Json.decodeFromString(ListSerializer(String.serializer()), raw).toSet()
  }.getOrDefault(emptySet())
}
