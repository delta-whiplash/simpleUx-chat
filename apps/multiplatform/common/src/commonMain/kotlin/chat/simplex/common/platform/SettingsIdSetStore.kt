package chat.simplex.common.platform

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// Shared persistence for SimpleUX "set of chat ids" preferences (pinned and
// starred chats). Encoding is JSON so chat ids never collide with a separator
// character; the wire format and storage keys are pinned by
// SettingsIdSetStoreTest and must stay byte-identical across releases so
// existing installs keep their state.
class SettingsIdSetStore(private val settings: Settings, private val key: String) {
  fun load(): Set<String> = decodeIdSet(settings.getStringOrNull(key))

  fun save(ids: Collection<String>) {
    settings.putString(key, encodeIdSet(ids))
  }
}

internal fun encodeIdSet(ids: Collection<String>): String =
  Json.encodeToString(ListSerializer(String.serializer()), ids.toList())

internal fun decodeIdSet(raw: String?): Set<String> {
  if (raw.isNullOrEmpty()) return emptySet()
  return runCatching {
    Json.decodeFromString(ListSerializer(String.serializer()), raw).toSet()
  }.getOrDefault(emptySet())
}
