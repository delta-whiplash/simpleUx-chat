package chat.simplex.common.platform

import com.russhwolf.settings.Settings

// SimpleUX local star ("favorite") persistence for chats, backed by its own
// moko multiplatform-settings instance. Kept separate from AppPreferences
// (model/SimpleXAPI.kt is byte-frozen) so the star state can be seeded into
// ChatModel.starredChatIds at startup and written through on toggle.
expect val starredChatsSettings: Settings

private const val STARRED_CHAT_IDS_KEY = "simpleux.starred.chatIds"

object StarredChatsPrefs {
  fun loadStarredChatIds(settings: Settings = starredChatsSettings): Set<String> =
    SettingsIdSetStore(settings, STARRED_CHAT_IDS_KEY).load()

  fun saveStarredChatIds(ids: Collection<String>, settings: Settings = starredChatsSettings) {
    SettingsIdSetStore(settings, STARRED_CHAT_IDS_KEY).save(ids)
  }
}
