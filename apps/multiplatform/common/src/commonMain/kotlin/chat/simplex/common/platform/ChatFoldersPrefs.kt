package chat.simplex.common.platform

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// SimpleUX Chat Folders persistence (issue #98 + #101): preset folders control
// which filter pills are visible on the chat list; custom folders carry their
// own chat membership (included minus excluded) and are stored locally only.

@Serializable
data class ChatFolder(
  val id: String,
  val name: String? = null,       // null = emoji-only display (or preset label)
  val emoji: String? = null,      // null = text-only display
  val filterKind: Int,            // UxFilterCategory.ordinal; -1 = custom folder
  val isVisible: Boolean = true,
  val order: Int = 0,
  val includedChatIds: Set<String> = emptySet(),  // #101: custom folder membership
  val excludedChatIds: Set<String> = emptySet()   // #101: excludes win over includes
) {
  val isCustom: Boolean get() = filterKind < 0

  // Telegram semantics: a chat is in the folder when it is included and not
  // excluded. Preset folders never consult membership.
  fun matchesChat(chatId: String): Boolean =
    isCustom && includedChatIds.contains(chatId) && !excludedChatIds.contains(chatId)
}

expect val chatFoldersSettings: Settings

private const val CHAT_FOLDERS_KEY = "simpleux_chat_folders.list"

object ChatFoldersPrefs {
  fun loadFolders(settings: Settings = chatFoldersSettings): List<ChatFolder> =
    decodeChatFolders(settings.getStringOrNull(CHAT_FOLDERS_KEY))

  fun saveFolders(folders: List<ChatFolder>, settings: Settings = chatFoldersSettings) {
    settings.putString(CHAT_FOLDERS_KEY, encodeChatFolders(folders))
  }

  fun saveFolder(folder: ChatFolder, settings: Settings = chatFoldersSettings) {
    val folders = loadFolders(settings)
    val updated = if (folders.any { it.id == folder.id }) {
      folders.map { if (it.id == folder.id) folder else it }
    } else {
      folders + folder
    }
    saveFolders(updated, settings)
  }

  fun deleteFolder(folderId: String, settings: Settings = chatFoldersSettings) {
    saveFolders(loadFolders(settings).filterNot { it.id == folderId }, settings)
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
