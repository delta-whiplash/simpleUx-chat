package chat.simplex.common.platform

import com.russhwolf.settings.Settings

// SimpleUX pin ("pin to top") persistence for chats (FB-14), backed by its own
// moko multiplatform-settings instance - same pattern as StarredChatsPrefs, and
// kept separate from AppPreferences for the same reason (model/SimpleXAPI.kt is
// byte-frozen; the settings store must not be touched during ChatModel class
// init, so ids are seeded via ChatModel.loadPersistedPinnedChats() from the
// platform app init instead). Pinning is a local display preference only: it is
// never sent to contacts or the core.
expect val pinnedChatsSettings: Settings

private const val PINNED_CHAT_IDS_KEY = "simpleux.pinned.chatIds"

object PinnedChatsPrefs {
  fun loadPinnedChatIds(settings: Settings = pinnedChatsSettings): Set<String> =
    SettingsIdSetStore(settings, PINNED_CHAT_IDS_KEY).load()

  fun savePinnedChatIds(ids: Collection<String>, settings: Settings = pinnedChatsSettings) {
    SettingsIdSetStore(settings, PINNED_CHAT_IDS_KEY).save(ids)
  }
}
