package chat.simplex.app

import chat.simplex.common.views.chatlist.nameSearchCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the on-device SimpleX-name search grammar implemented by
 * `nameSearchCandidate` in views/chatlist/ChatListView.kt (search field only - the message
 * parser and wire format are untouched).
 *
 * Grammar (mirrors nameLabelP/mkDomain in SimplexName.hs):
 *  - optional `@`/`#` prefix, preserved in the returned candidate;
 *  - dot-separated ASCII labels, each 1..63 chars matching [a-zA-Z0-9]+(-[a-zA-Z0-9]+)*;
 *  - a dotless word of >= 5 chars is completed with the default top-level part "testing";
 *  - anything else (invalid chars, empty/short dotless word) yields null.
 *
 * The private helper `isNameLabel` is exercised indirectly through these cases.
 */
class NameSearchCandidateTest {

  // --- dotless word: completed with the default top-level part ---

  @Test
  fun dotlessWordGetsDefaultTLD() {
    assertEquals("alice.testing", nameSearchCandidate("alice"))
    assertEquals("ALICE.testing", nameSearchCandidate("ALICE"))
    assertEquals("12345.testing", nameSearchCandidate("12345"))
  }

  @Test
  fun exactlyFiveCharsIsLongEnough() {
    assertEquals("abcde.testing", nameSearchCandidate("abcde"))
    assertEquals("ab-cd.testing", nameSearchCandidate("ab-cd"))
  }

  @Test
  fun shorterDotlessWordYieldsNull() {
    assertNull(nameSearchCandidate("abcd"))
    assertNull(nameSearchCandidate("a"))
  }

  @Test
  fun prefixIsKeptAndCompletionApplied() {
    assertEquals("@alice.testing", nameSearchCandidate("@alice"))
    assertEquals("#alice.testing", nameSearchCandidate("#alice"))
  }

  @Test
  fun prefixedShortWordYieldsNull() {
    assertNull(nameSearchCandidate("@a"))
    assertNull(nameSearchCandidate("#ab"))
  }

  @Test
  fun inputIsTrimmedBeforeParsing() {
    assertEquals("alice.testing", nameSearchCandidate("  alice  "))
    assertEquals("@alice.testing", nameSearchCandidate("\t@alice\n"))
  }

  // --- multi-label: returned as-is (already has a top-level part) ---

  @Test
  fun multiLabelNameReturnedVerbatim() {
    assertEquals("alice.chat", nameSearchCandidate("alice.chat"))
    assertEquals("a.b.c", nameSearchCandidate("a.b.c"))
    assertEquals("al.ice", nameSearchCandidate("al.ice"))
    assertEquals("123.45", nameSearchCandidate("123.45"))
  }

  @Test
  fun prefixedMultiLabelNameReturnedVerbatim() {
    assertEquals("@alice.chat", nameSearchCandidate("@alice.chat"))
    assertEquals("#a.b", nameSearchCandidate("#a.b"))
  }

  @Test
  fun multiLabelTrimmedVerbatim() {
    assertEquals("@alice.chat", nameSearchCandidate("  @alice.chat  "))
  }

  @Test
  fun multiLabelWithShortLabelsIsValid() {
    // MIN_NAME_LENGTH only applies to the dotless-completion path
    assertEquals("a.b", nameSearchCandidate("a.b"))
  }

  // --- label validation (isNameLabel via the candidate result) ---

  @Test
  fun hyphenatedLabelsAreValid() {
    assertEquals("al-ice.testing", nameSearchCandidate("al-ice"))
    assertEquals("al-ice.co", nameSearchCandidate("al-ice.co"))
  }

  @Test
  fun hyphenOnlyEdgesAreInvalid() {
    assertNull(nameSearchCandidate("-alice"))
    assertNull(nameSearchCandidate("alice-"))
    assertNull(nameSearchCandidate("al--ice"))
    assertNull(nameSearchCandidate("a.-b.c"))
  }

  @Test
  fun invalidCharactersYieldNull() {
    assertNull(nameSearchCandidate("alice chat"))
    assertNull(nameSearchCandidate("al_ce"))
    assertNull(nameSearchCandidate("alice!"))
    assertNull(nameSearchCandidate("alicé"))
    assertNull(nameSearchCandidate("alice/ch.test"))
  }

  @Test
  fun emptyLabelInMultiLabelYieldsNull() {
    assertNull(nameSearchCandidate(".alice"))
    assertNull(nameSearchCandidate("alice."))
    assertNull(nameSearchCandidate("al..ice"))
    assertNull(nameSearchCandidate("@alice."))
  }

  @Test
  fun labelLongerThan63CharsYieldsNull() {
    val tooLong = "a".repeat(64)
    assertNull(nameSearchCandidate(tooLong))
    // also rejected inside a multi-label name
    assertNull(nameSearchCandidate("a.$tooLong"))
  }

  @Test
  fun labelOfExactly63CharsIsValid() {
    val max = "a".repeat(63)
    assertEquals("$max.testing", nameSearchCandidate(max))
    assertEquals("x.$max", nameSearchCandidate("x.$max"))
  }

  // --- empty / degenerate input ---

  @Test
  fun emptyAndBlankYieldNull() {
    assertNull(nameSearchCandidate(""))
    assertNull(nameSearchCandidate("   "))
    assertNull(nameSearchCandidate("\t\n"))
  }

  @Test
  fun lonePrefixYieldsNull() {
    assertNull(nameSearchCandidate("@"))
    assertNull(nameSearchCandidate("#"))
    assertNull(nameSearchCandidate(" @ "))
  }

  @Test
  fun interiorSpaceAfterPrefixYieldsNull() {
    assertNull(nameSearchCandidate("@ alice"))
  }

  @Test
  fun prefixOnlyAppliesToFirstCharacter() {
    // second @ is an invalid label character
    assertNull(nameSearchCandidate("@@alice"))
    assertNull(nameSearchCandidate("ali@ce"))
  }
}
