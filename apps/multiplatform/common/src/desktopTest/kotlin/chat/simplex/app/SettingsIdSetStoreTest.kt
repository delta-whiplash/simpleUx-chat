package chat.simplex.app

import chat.simplex.common.platform.PinnedChatsPrefs
import chat.simplex.common.platform.SettingsIdSetStore
import chat.simplex.common.platform.StarredChatsPrefs
import com.russhwolf.settings.PropertiesSettings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

// use this command for testing:
// ./gradlew :common:desktopTest --tests "chat.simplex.app.SettingsIdSetStoreTest"

// Pins the persistence contract shared by PinnedChatsPrefs and StarredChatsPrefs (#106).
// The stored format is part of the upgrade surface: existing installs keep their pinned
// and starred chats across releases, so the encoding and the keys must not change.
class SettingsIdSetStoreTest {
  private fun newSettings(props: Properties): PropertiesSettings = PropertiesSettings(props) {}

  @Test
  fun loadOnEmptyStoreReturnsEmptySet() {
    assertEquals(emptySet(), SettingsIdSetStore(newSettings(Properties()), "k").load())
  }

  @Test
  fun savesAndLoadsIdSetRoundTrip() {
    val store = SettingsIdSetStore(newSettings(Properties()), "k")
    store.save(listOf("1", "42", "7"))
    assertEquals(setOf("1", "42", "7"), store.load())
  }

  @Test
  fun saveOverwritesPreviousValue() {
    val store = SettingsIdSetStore(newSettings(Properties()), "k")
    store.save(listOf("1", "2"))
    store.save(listOf("2"))
    assertEquals(setOf("2"), store.load())
  }

  @Test
  fun idsThatLookLikeSeparatorsSurviveRoundTrip() {
    val store = SettingsIdSetStore(newSettings(Properties()), "k")
    store.save(listOf("a,b", "c|d", "e\"f", " chat with spaces "))
    assertEquals(setOf("a,b", "c|d", "e\"f", " chat with spaces "), store.load())
  }

  // The format pin: a JSON array of strings on the wire.
  @Test
  fun storedFormatIsJsonStringArray() {
    val props = Properties()
    SettingsIdSetStore(newSettings(props), "k").save(listOf("a", "b"))
    assertEquals("[\"a\",\"b\"]", props.getProperty("k"))
  }

  // Upgrade-compat pins: go through the public objects so their historic storage
  // keys can't silently change under existing installs.
  @Test
  fun pinnedChatsPrefsPersistsUnderItsHistoricKey() {
    val props = Properties()
    PinnedChatsPrefs.savePinnedChatIds(listOf("42"), newSettings(props))
    assertEquals("[\"42\"]", props.getProperty("simpleux.pinned.chatIds"))
  }

  @Test
  fun starredChatsPrefsPersistsUnderItsHistoricKey() {
    val props = Properties()
    StarredChatsPrefs.saveStarredChatIds(listOf("42"), newSettings(props))
    assertEquals("[\"42\"]", props.getProperty("simpleux.starred.chatIds"))
  }

  // Simulate an app restart: serialize the properties out, read them back into a
  // fresh instance, and check the ids survive.
  @Test
  fun idsSurviveSettingsRestart() {
    val props = Properties()
    val store = SettingsIdSetStore(newSettings(props), "k")
    store.save(listOf("1", "42", "7"))

    val bytes = ByteArrayOutputStream().use { out ->
      props.store(out, "")
      out.toByteArray()
    }
    val reloadedProps = Properties().also { it.load(ByteArrayInputStream(bytes)) }
    assertEquals(setOf("1", "42", "7"), SettingsIdSetStore(newSettings(reloadedProps), "k").load())

    // Writing again goes through the same key
    store.save(listOf("42"))
    assertEquals(setOf("42"), store.load())
  }

  @Test
  fun corruptValueReadsBackAsEmptySet() {
    val props = Properties().also { it.setProperty("k", "not json at all]") }
    assertEquals(emptySet(), SettingsIdSetStore(newSettings(props), "k").load())
  }

  @Test
  fun emptyStringReadsBackAsEmptySet() {
    val props = Properties().also { it.setProperty("k", "") }
    assertEquals(emptySet(), SettingsIdSetStore(newSettings(props), "k").load())
  }
}
