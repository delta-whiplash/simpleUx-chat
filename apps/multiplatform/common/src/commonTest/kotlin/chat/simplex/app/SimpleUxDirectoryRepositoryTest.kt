package chat.simplex.app

import chat.simplex.common.views.chatlist.parseDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the pure JSON-parsing layer of SimpleUxDirectoryRepository (extracted from the network
 * fetch so it can be tested offline).
 *
 * The listing shape is the one served by https://directory.simplex.chat/data/listing.json:
 * an `entries` array of objects with displayName, groupLink{connShortLink,connFullLink},
 * welcomeMessage/shortDescr arrays of {text}, entryType{groupType|type,summary{currentMembers|members}}
 * and imageFile.
 *
 * `description == null` in the parsed entry means "no text provided by the directory" - the
 * repository mapping substitutes the localized default description at render time.
 * `membersCount == null` means "no positive member count" - the mapping falls back to the
 * localized "public group" string.
 *
 * Robustness contract (#74): one structurally corrupt entry is skipped and parsing continues with
 * the rest of the listing; an explicit JSON null in any optional field reads as absent (never as
 * the literal string "null").
 */
class SimpleUxDirectoryRepositoryTest {

  // --- happy path ---

  @Test
  fun happyPathParsesFullEntry() {
    val json = """
      {
        "entries": [
          {
            "displayName": "Foo Chat",
            "groupLink": { "connShortLink": "https://smp.simplex.im/f#short" },
            "welcomeMessage": [ { "text": "Welcome to Foo" } ],
            "entryType": { "groupType": "group", "summary": { "currentMembers": "123" } },
            "imageFile": "foo.png"
          }
        ]
      }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)

    assertEquals(1, entries.size)
    val e = entries[0]
    assertEquals("Foo Chat", e.name)
    assertEquals("https://smp.simplex.im/f#short", e.link)
    assertEquals("Welcome to Foo", e.description)
    assertTrue(!e.isChannel)
    assertEquals(123, e.membersCount)
    assertEquals("https://directory.simplex.chat/data/foo.png", e.imageUrl)
  }

  @Test
  fun shortLinkPreferredOverFullLink() {
    val json = """
      { "entries": [ {
          "displayName": "Dual",
          "groupLink": { "connShortLink": "https://smp.simplex.im/d#s", "connFullLink": "https://smp.simplex.im/d#full" }
      } ] }
    """.trimIndent()

    assertEquals("https://smp.simplex.im/d#s", parseDirectoryEntries(json)[0].link)
  }

  @Test
  fun fullLinkUsedWhenShortLinkMissing() {
    val json = """
      { "entries": [ {
          "displayName": "Full",
          "groupLink": { "connFullLink": "https://smp.simplex.im/f#full" }
      } ] }
    """.trimIndent()

    assertEquals("https://smp.simplex.im/f#full", parseDirectoryEntries(json)[0].link)
  }

  // --- description derivation ---

  @Test
  fun welcomePartsAreConcatenated() {
    val json = """
      { "entries": [ {
          "displayName": "W",
          "groupLink": { "connShortLink": "https://smp.simplex.im/w" },
          "welcomeMessage": [ { "text": "One " }, { "text": "Two" } ]
      } ] }
    """.trimIndent()

    assertEquals("One Two", parseDirectoryEntries(json)[0].description)
  }

  @Test
  fun shortDescrUsedWhenWelcomeMissing() {
    val json = """
      { "entries": [ {
          "displayName": "S",
          "groupLink": { "connShortLink": "https://smp.simplex.im/s" },
          "shortDescr": [ { "text": "Short description" } ]
      } ] }
    """.trimIndent()

    assertEquals("Short description", parseDirectoryEntries(json)[0].description)
  }

  @Test
  fun blankWelcomeFallsThroughToShortDescr() {
    val json = """
      { "entries": [ {
          "displayName": "B",
          "groupLink": { "connShortLink": "https://smp.simplex.im/b" },
          "welcomeMessage": [ { "text": "   " } ],
          "shortDescr": [ { "text": "Fallback" } ]
      } ] }
    """.trimIndent()

    assertEquals("Fallback", parseDirectoryEntries(json)[0].description)
  }

  @Test
  fun noTextYieldsNullDescription() {
    val json = """
      { "entries": [ {
          "displayName": "N",
          "groupLink": { "connShortLink": "https://smp.simplex.im/n" }
      } ] }
    """.trimIndent()

    assertNull(parseDirectoryEntries(json)[0].description)
  }

  @Test
  fun emptyWelcomeArrayYieldsNullDescription() {
    val json = """
      { "entries": [ {
          "displayName": "E",
          "groupLink": { "connShortLink": "https://smp.simplex.im/e" },
          "welcomeMessage": []
      } ] }
    """.trimIndent()

    assertNull(parseDirectoryEntries(json)[0].description)
  }

  @Test
  fun descriptionSkipsBlankAndLinkToJoinLines() {
    val json = """
      { "entries": [ {
          "displayName": "L",
          "groupLink": { "connShortLink": "https://smp.simplex.im/l" },
          "welcomeMessage": [ { "text": "\nLink to join: https://smp.simplex.im/l\nRules here" } ]
      } ] }
    """.trimIndent()

    assertEquals("Rules here", parseDirectoryEntries(json)[0].description)
  }

  @Test
  fun allLinesFilteredFallsBackToWholeTrimmedText() {
    val json = """
      { "entries": [ {
          "displayName": "F",
          "groupLink": { "connShortLink": "https://smp.simplex.im/f" },
          "welcomeMessage": [ { "text": "Link to join: https://smp.simplex.im/f" } ]
      } ] }
    """.trimIndent()

    assertEquals("Link to join: https://smp.simplex.im/f", parseDirectoryEntries(json)[0].description)
  }

  // --- category / type ---

  @Test
  fun channelTypeIsRecognized() {
    val json = """
      { "entries": [ {
          "displayName": "C",
          "groupLink": { "connShortLink": "https://smp.simplex.im/c" },
          "entryType": { "groupType": "channel" }
      } ] }
    """.trimIndent()

    assertTrue(parseDirectoryEntries(json)[0].isChannel)
  }

  @Test
  fun channelTypeIsCaseInsensitiveAndTypeFieldIsFallback() {
    val json = """
      { "entries": [
          { "displayName": "C1", "groupLink": { "connShortLink": "https://smp.simplex.im/c1" }, "entryType": { "type": "Channel" } },
          { "displayName": "C2", "groupLink": { "connShortLink": "https://smp.simplex.im/c2" }, "entryType": { "groupType": "CHANNEL" } }
      ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertTrue(entries[0].isChannel)
    assertTrue(entries[1].isChannel)
  }

  @Test
  fun missingEntryTypeIsPlainGroup() {
    val json = """
      { "entries": [ { "displayName": "G", "groupLink": { "connShortLink": "https://smp.simplex.im/g" } } ] }
    """.trimIndent()

    assertTrue(!parseDirectoryEntries(json)[0].isChannel)
  }

  // --- members count ---

  @Test
  fun currentMembersParsed() {
    val json = """
      { "entries": [ {
          "displayName": "M",
          "groupLink": { "connShortLink": "https://smp.simplex.im/m" },
          "entryType": { "summary": { "currentMembers": "42" } }
      } ] }
    """.trimIndent()

    assertEquals(42, parseDirectoryEntries(json)[0].membersCount)
  }

  @Test
  fun membersFieldIsFallbackForCurrentMembers() {
    val json = """
      { "entries": [ {
          "displayName": "M2",
          "groupLink": { "connShortLink": "https://smp.simplex.im/m2" },
          "entryType": { "summary": { "currentMembers": "not-a-number", "members": "7" } }
      } ] }
    """.trimIndent()

    assertEquals(7, parseDirectoryEntries(json)[0].membersCount)
  }

  @Test
  fun zeroNegativeOrMissingMembersYieldNull() {
    val json = """
      { "entries": [
          { "displayName": "Z", "groupLink": { "connShortLink": "https://smp.simplex.im/z" }, "entryType": { "summary": { "currentMembers": "0" } } },
          { "displayName": "V", "groupLink": { "connShortLink": "https://smp.simplex.im/v" }, "entryType": { "summary": { "members": "-5" } } },
          { "displayName": "X", "groupLink": { "connShortLink": "https://smp.simplex.im/x" }, "entryType": { "summary": {} } }
      ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertEquals(3, entries.size)
    assertNull(entries[0].membersCount)
    assertNull(entries[1].membersCount)
    assertNull(entries[2].membersCount)
  }

  // --- image ---

  @Test
  fun missingOrBlankImageFileYieldsNullImageUrl() {
    val json = """
      { "entries": [
          { "displayName": "I1", "groupLink": { "connShortLink": "https://smp.simplex.im/i1" } },
          { "displayName": "I2", "groupLink": { "connShortLink": "https://smp.simplex.im/i2" }, "imageFile": "  " }
      ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertNull(entries[0].imageUrl)
    assertNull(entries[1].imageUrl)
  }

  // --- entry skipping / degenerate input ---

  @Test
  fun entryWithoutDisplayNameIsSkipped() {
    val json = """
      { "entries": [
          { "groupLink": { "connShortLink": "https://smp.simplex.im/no-name" } },
          { "displayName": "Good", "groupLink": { "connShortLink": "https://smp.simplex.im/good" } }
      ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertEquals(1, entries.size)
    assertEquals("Good", entries[0].name)
  }

  @Test
  fun entryWithoutLinkIsSkipped() {
    val json = """
      { "entries": [
          { "displayName": "NoLink" },
          { "displayName": "Good", "groupLink": { "connFullLink": "https://smp.simplex.im/good" } }
      ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertEquals(1, entries.size)
    assertEquals("Good", entries[0].name)
  }

  @Test
  fun groupLinkWithoutLinkFieldsIsSkipped() {
    val json = """
      { "entries": [ { "displayName": "EmptyLink", "groupLink": {} } ] }
    """.trimIndent()

    assertEquals(0, parseDirectoryEntries(json).size)
  }

  @Test
  fun multipleEntriesParsedInOrder() {
    val json = """
      { "entries": [
          { "displayName": "A", "groupLink": { "connShortLink": "https://smp.simplex.im/a" } },
          { "displayName": "B", "groupLink": { "connShortLink": "https://smp.simplex.im/b" } },
          { "displayName": "C", "groupLink": { "connShortLink": "https://smp.simplex.im/c" } }
      ] }
    """.trimIndent()

    assertEquals(listOf("A", "B", "C"), parseDirectoryEntries(json).map { it.name })
  }

  @Test
  fun emptyEntriesArrayYieldsEmptyList() {
    assertEquals(0, parseDirectoryEntries("""{ "entries": [] }""").size)
  }

  @Test
  fun missingEntriesKeyYieldsEmptyList() {
    assertEquals(0, parseDirectoryEntries("""{ "something": 1 }""").size)
  }

  @Test
  fun emptyObjectYieldsEmptyList() {
    assertEquals(0, parseDirectoryEntries("{}").size)
  }

  @Test
  fun malformedJSONYieldsEmptyListInsteadOfCrashing() {
    assertEquals(0, parseDirectoryEntries("this is not json").size)
    assertEquals(0, parseDirectoryEntries("").size)
    assertEquals(0, parseDirectoryEntries("[1, 2, 3]").size) // root is not an object
    assertEquals(0, parseDirectoryEntries("""{ "entries": "not-an-array" }""").size)
  }

  @Test
  fun entriesOfWrongShapeYieldEmptyListInsteadOfCrashing() {
    // entries is an array but elements are not objects
    assertEquals(0, parseDirectoryEntries("""{ "entries": [1, "two"] }""").size)
  }

  // --- corrupt-entry isolation (#74): a broken entry is skipped, parsing continues ---

  @Test
  fun corruptFirstEntryDoesNotDropLaterEntries() {
    val json = """
      { "entries": [
          { "displayName": "Corrupt", "groupLink": { "connShortLink": "https://smp.simplex.im/c" }, "welcomeMessage": [ "not-an-object" ] },
          { "displayName": "Good1", "groupLink": { "connShortLink": "https://smp.simplex.im/g1" } },
          { "displayName": "Good2", "groupLink": { "connShortLink": "https://smp.simplex.im/g2" } }
      ] }
    """.trimIndent()

    assertEquals(listOf("Good1", "Good2"), parseDirectoryEntries(json).map { it.name })
  }

  @Test
  fun corruptMiddleEntryIsSkippedAndParsingContinues() {
    val json = """
      { "entries": [
          { "displayName": "A", "groupLink": { "connShortLink": "https://smp.simplex.im/a" } },
          { "displayName": "Corrupt", "groupLink": { "connShortLink": "https://smp.simplex.im/c" }, "welcomeMessage": [ "not-an-object" ] },
          { "displayName": "B", "groupLink": { "connShortLink": "https://smp.simplex.im/b" } }
      ] }
    """.trimIndent()

    assertEquals(listOf("A", "B"), parseDirectoryEntries(json).map { it.name })
  }

  @Test
  fun allEntriesCorruptYieldEmptyListWithoutThrowing() {
    // Corrupt in three different ways: broken welcomeMessage array part, non-object groupLink,
    // element that is not an object at all.
    val json = """
      { "entries": [
          { "displayName": "C1", "groupLink": { "connShortLink": "https://smp.simplex.im/c1" }, "welcomeMessage": [ "not-an-object" ] },
          { "displayName": "C2", "groupLink": "not-an-object" },
          "not-an-object"
      ] }
    """.trimIndent()

    assertEquals(0, parseDirectoryEntries(json).size)
  }

  // --- explicit JSON nulls (#74): JsonNull must read as absent, never as the string "null" ---

  @Test
  fun nullDisplayNameSkipsEntry() {
    // Fallback decision: name is required - an entry with no usable name (absent, null or blank
    // displayName) cannot be listed and is skipped, never rendered named "null".
    val json = """
      { "entries": [
          { "displayName": null, "groupLink": { "connShortLink": "https://smp.simplex.im/null-name" } },
          { "displayName": "Good", "groupLink": { "connShortLink": "https://smp.simplex.im/good" } }
      ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertEquals(1, entries.size)
    assertEquals("Good", entries[0].name)
  }

  @Test
  fun blankDisplayNameIsSkippedToo() {
    val json = """
      { "entries": [
          { "displayName": "   ", "groupLink": { "connShortLink": "https://smp.simplex.im/blank" } },
          { "displayName": "Good", "groupLink": { "connShortLink": "https://smp.simplex.im/good" } }
      ] }
    """.trimIndent()

    assertEquals(listOf("Good"), parseDirectoryEntries(json).map { it.name })
  }

  @Test
  fun nullShortLinkFallsBackToFullLink() {
    // Fallback decision: short link preferred, full link is the fallback - a null short link must
    // count as absent, not produce a broken ".../null" link.
    val json = """
      { "entries": [ {
          "displayName": "NullShort",
          "groupLink": { "connShortLink": null, "connFullLink": "https://smp.simplex.im/full" }
      } ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertEquals(1, entries.size)
    assertEquals("https://smp.simplex.im/full", entries[0].link)
  }

  @Test
  fun nullShortAndFullLinkSkipEntry() {
    val json = """
      { "entries": [ {
          "displayName": "NullLinks",
          "groupLink": { "connShortLink": null, "connFullLink": null }
      } ] }
    """.trimIndent()

    assertEquals(0, parseDirectoryEntries(json).size)
  }

  @Test
  fun explicitNullsInEveryOptionalFieldParseCleanly() {
    val json = """
      { "entries": [ {
          "displayName": "Nulls",
          "groupLink": { "connShortLink": null, "connFullLink": "https://smp.simplex.im/full" },
          "welcomeMessage": null,
          "shortDescr": null,
          "entryType": { "groupType": null, "type": null, "summary": { "currentMembers": null, "members": null } },
          "imageFile": null
      } ] }
    """.trimIndent()

    val entries = parseDirectoryEntries(json)
    assertEquals(1, entries.size)
    val e = entries[0]
    assertEquals("Nulls", e.name)
    assertEquals("https://smp.simplex.im/full", e.link) // no broken ".../null" link
    assertNull(e.description) // null welcome/shortDescr read as absent -> null description
    assertTrue(!e.isChannel) // null groupType/type reads as plain group, not the string "null"
    assertNull(e.membersCount)
    assertNull(e.imageUrl) // no ".../data/null" image URL
  }

  @Test
  fun nullGroupTypeFallsThroughToTypeField() {
    val json = """
      { "entries": [ {
          "displayName": "NullGroupType",
          "groupLink": { "connShortLink": "https://smp.simplex.im/n" },
          "entryType": { "groupType": null, "type": "channel" }
      } ] }
    """.trimIndent()

    assertTrue(parseDirectoryEntries(json)[0].isChannel)
  }

  @Test
  fun nullTextPartsAreSkippedAndDescriptionFallsBack() {
    val json = """
      { "entries": [ {
          "displayName": "NullText",
          "groupLink": { "connShortLink": "https://smp.simplex.im/nt" },
          "welcomeMessage": [ { "text": null }, { "text": "  " } ],
          "shortDescr": [ { "text": "Fallback" } ]
      } ] }
    """.trimIndent()

    assertEquals("Fallback", parseDirectoryEntries(json)[0].description)
  }

  @Test
  fun unknownKeysAreIgnored() {
    val json = """
      { "generatedAt": "2026-01-01", "entries": [
          { "displayName": "U", "groupLink": { "connShortLink": "https://smp.simplex.im/u", "x": 1 }, "unknownField": { "deep": [1,2] } }
      ] }
    """.trimIndent()

    assertEquals(listOf("U"), parseDirectoryEntries(json).map { it.name })
  }
}
