package chat.simplex.app

import chat.simplex.common.platform.StarredChatsPrefs
import chat.simplex.common.platform.decodeStarredChatIds
import chat.simplex.common.platform.encodeStarredChatIds
import com.russhwolf.settings.PropertiesSettings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

// use this command for testing:
// ./gradlew desktopTest
class StarredChatsPrefsTest {
  private fun newSettings(props: Properties): PropertiesSettings = PropertiesSettings(props) {}

  @Test
  fun starredChatIdsRoundTripThroughSettings() {
    val props = Properties()
    val settings = newSettings(props)

    assertEquals(emptySet(), StarredChatsPrefs.loadStarredChatIds(settings))

    StarredChatsPrefs.saveStarredChatIds(listOf("1", "42", "7"), settings)
    // Simulate an app restart: serialize the properties out, read them back into a fresh instance
    val bytes = ByteArrayOutputStream().use { out ->
      props.store(out, "")
      out.toByteArray()
    }
    val reloadedProps = Properties().also { it.load(ByteArrayInputStream(bytes)) }
    assertEquals(setOf("1", "42", "7"), StarredChatsPrefs.loadStarredChatIds(newSettings(reloadedProps)))

    // Unstarring a chat writes through the same key
    StarredChatsPrefs.saveStarredChatIds(listOf("42"), settings)
    assertEquals(setOf("42"), StarredChatsPrefs.loadStarredChatIds(settings))
  }

  @Test
  fun encodeDecodeStarredChatIdsPreservesValues() {
    val ids = listOf("1", "<@42", "chat with spaces")
    assertEquals(ids.toSet(), decodeStarredChatIds(encodeStarredChatIds(ids)))
    assertEquals(emptySet(), decodeStarredChatIds(encodeStarredChatIds(emptyList())))
  }

  @Test
  fun decodeStarredChatIdsToleratesCorruptInput() {
    assertEquals(emptySet(), decodeStarredChatIds(null))
    assertEquals(emptySet(), decodeStarredChatIds(""))
    assertEquals(emptySet(), decodeStarredChatIds("not json at all]"))
  }
}
