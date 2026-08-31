package chat.simplex.common.platform

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// SimpleUX Chat Folders persistence (issue #98): allows users to customize which
// filter categories are visible on the chat list, reorder them, and create custom
// folders with names/emojis. Backed by moko multiplatform-settings (same pattern
// as PinnedChatsPrefs/StarredChatsPrefs). Stored locally only — never synced.

@Serializable
data class ChatFolder(
  val id: String,
  val name: String? = null,       // null = emoji-only display
  val emoji: String? = null,      // null = text-only display
  val filterKind: Int,            // UxFilterCategory.ordinal
  val isVisible: Boolean = true,
  val order: Int = 0
)

expect val chatFoldersSettings: Settings

private const val CHAT_FOLDERS_KEY = "simpleux_chat_folders.list"

object ChatFoldersPrefs {
  fun loadFolders(settings: Settings = chatFoldersSettings): List<ChatFolder> =
    decodeChatFolders(settings.getStringOrNull(CHAT_FOLDERS_KEY))

  fun saveFolders(folders: List<ChatFolder>, settings: Settings = chatFoldersSettings) {
    settings.putString(CHAT_FOLDERS_KEY, encodeChatFolders(folders))
  }
}

internal fun encodeChatFolders(folders: List<ChatFolder>): String =
  Json.encodeToString(ListSerializer(ChatFolder.serializer()), folders)

internal fun decodeChatFolders(raw: String?): List<ChatFolder> {
  if (raw.isNullOrEmpty()) return defaultChatFolders()
  return runCatching {
    Json.decodeFromString(ListSerializer(ChatFolder.serializer()), raw)
  }.getOrDefault(defaultChatFolders())
}

// Default folders: ALL (always visible, order=0), UNREAD (visible, order=1),
// others hidden (order=2,3,4). User can toggle visibility and reorder.
internal fun defaultChatFolders(): List<ChatFolder> = listOf(
  ChatFolder(id = "all", filterKind = 0, isVisible = true, order = 0),   // ALL
  ChatFolder(id = "unread", filterKind = 1, isVisible = true, order = 1), // UNREAD
  ChatFolder(id = "direct", filterKind = 2, isVisible = false, order = 2), // DIRECT
  ChatFolder(id = "groups", filterKind = 3, isVisible = false, order = 3), // GROUPS
  ChatFolder(id = "favorites", filterKind = 4, isVisible = false, order = 4) // FAVORITES
)
