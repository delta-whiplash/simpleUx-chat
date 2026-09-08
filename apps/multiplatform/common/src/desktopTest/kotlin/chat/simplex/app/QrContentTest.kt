package chat.simplex.app

import chat.simplex.common.model.SimplexDomain
import chat.simplex.common.model.SimplexLinkType
import chat.simplex.common.model.SimplexNameInfo
import chat.simplex.common.model.SimplexNameType
import chat.simplex.common.model.SimplexTLD
import chat.simplex.common.views.newchat.ConnectTarget
import chat.simplex.common.views.ux.camera.QrContent
import chat.simplex.common.views.ux.camera.classifyQrContent
import chat.simplex.common.views.ux.camera.isDesktopLink
import chat.simplex.common.views.ux.camera.isHttpUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Classifier tests for the quick camera's universal QR routing.
//
// The production SimpleX detection (strConnectTarget -> chatParseMarkdown)
// bottoms out in a JNI call into the Haskell core, and desktopTest runs
// WITHOUT the native library (it is only loaded by the packaged desktop app
// at startup). The tests therefore exercise the classifier's injectable
// detector seam: SimpleX cases stub a detector that returns the same
// ConnectTarget shapes strConnectTarget produces, and the URL/Text cases run
// the production classifier with a detector returning null, which is exactly
// what strConnectTarget yields for those payloads.
//
// use this command for testing:
// ./gradlew desktopTest
class QrContentTest {
  private val notSimplex: (String) -> ConnectTarget? = { null }
  private val detectorArgs = mutableListOf<String>()
  private val recordingNotSimplex: (String) -> ConnectTarget? = { arg ->
    detectorArgs.add(arg)
    null
  }

  @Test
  fun simplexDetectorResultRoutesToSimpleXTarget() {
    // Contact, group and channel links share one detector seam; any non-null
    // ConnectTarget must surface as SimpleXTarget carrying that same target.
    val contact = ConnectTarget.Link("https://simplex.chat/contact#/abc-def", SimplexLinkType.contact, "Alice")
    val group = ConnectTarget.Link("https://simplex.chat/group#/abc-def", SimplexLinkType.group, "Team")
    val channel = ConnectTarget.Link("https://simplex.chat/channel#/abc-def", SimplexLinkType.channel, "News")
    val name = ConnectTarget.Name(
      "@delta",
      SimplexNameInfo(SimplexNameType.contact, SimplexDomain(SimplexTLD.simplex, "simplex", emptyList()))
    )
    listOf(
      "https://simplex.chat/contact#/abc-def" to contact,
      "https://simplex.chat/group#/abc-def" to group,
      "https://simplex.chat/channel#/abc-def" to channel,
      "@delta" to name
    ).forEach { (payload, target) ->
      val result = classifyQrContent(payload) { target }
      assertEquals(QrContent.SimpleXTarget(target), result)
      assertSame(target, (result as QrContent.SimpleXTarget).target)
    }
  }

  @Test
  fun simplexDetectionWinsOverUrlShape() {
    // A SimpleX link distributed as a web URL must reach the connect flow,
    // not the browser, even though it has a perfectly URL-shaped host.
    val target = ConnectTarget.Link("https://simplex.chat/invitation#/?v=1&s=abc", SimplexLinkType.invitation, "Invitation")
    val result = classifyQrContent("https://simplex.chat/invitation#/?v=1&s=abc") { target }
    assertEquals(QrContent.SimpleXTarget(target), result)
  }

  @Test
  fun httpsUrlClassifiesAsUrl() {
    assertEquals(
      QrContent.Url("https://example.com/page?q=1"),
      classifyQrContent("https://example.com/page?q=1", notSimplex)
    )
  }

  @Test
  fun httpUrlClassifiesAsUrl() {
    assertEquals(
      QrContent.Url("http://192.168.1.10:8080/status"),
      classifyQrContent("http://192.168.1.10:8080/status", notSimplex)
    )
  }

  @Test
  fun plainSentenceClassifiesAsText() {
    assertEquals(
      QrContent.Text("The register code is 4821, see you at the workshop"),
      classifyQrContent("The register code is 4821, see you at the workshop", notSimplex)
    )
  }

  @Test
  fun emptyStringClassifiesAsEmptyText() {
    // Documented choice: the classifier is total; a blank payload is Text("").
    // The camera analyzer never reports one in practice.
    assertEquals(QrContent.Text(""), classifyQrContent("", notSimplex))
    assertEquals(QrContent.Text(""), classifyQrContent("   \n", notSimplex))
  }

  @Test
  fun simplexLookingButMalformedPayloadsFallBackOnClassifierShape() {
    // Production semantics: strConnectTarget yields null for these (here:
    // the stub), so routing is decided by the URL shape check.
    // A bare simplex URI without a parseable link -> Text.
    assertEquals(QrContent.Text("simplex:garbage"), classifyQrContent("simplex:garbage", notSimplex))
    // A simplex.chat URL with no link payload -> Url (it really is a URL;
    // whether it is a SimpleX link is the detector's call, not the shape check's).
    assertEquals(
      QrContent.Url("https://simplex.chat/contact"),
      classifyQrContent("https://simplex.chat/contact", notSimplex)
    )
  }

  @Test
  fun payloadIsTrimmedBeforeDetectionAndStored() {
    assertEquals(
      QrContent.Url("https://example.com/a"),
      classifyQrContent("  https://example.com/a  ", notSimplex)
    )
    assertEquals(
      QrContent.Text("hello world"),
      classifyQrContent("  hello world\t", notSimplex)
    )
    // The detector receives the trimmed payload, and onQrCode-style hand-offs
    // downstream can rely on that.
    detectorArgs.clear()
    classifyQrContent("  @delta  ", recordingNotSimplex)
    assertEquals(listOf("@delta"), detectorArgs)
  }

  @Test
  fun desktopInvitationClassifiesAsDesktopLink() {
    // #159: an XRCP desktop-linking invitation (format per
    // Simplex/RemoteControl/Invitation.hs) used to land in the generic Text
    // bucket, so the Scan tab never routed it to the desktop-connect feature.
    val invitation = "xrcp:/abc@192.168.1.5:55072#/?v=2&ssig=x&idsig=y"
    assertEquals(QrContent.DesktopLink(invitation), classifyQrContent(invitation, notSimplex))
  }

  @Test
  fun desktopLinkCheckRunsBeforeTheSimplexDetector() {
    // The xrcp:/ prefix is decided before the detector seam runs: even a
    // detector that (wrongly) claims the payload cannot steal it from the
    // desktop-connect routing.
    val invitation = "xrcp:/abc@192.168.1.5:55072#/?v=2&ssig=x&idsig=y"
    val target = ConnectTarget.Link("https://simplex.chat/invitation#/?v=1&s=abc", SimplexLinkType.invitation, "Invitation")
    assertEquals(QrContent.DesktopLink(invitation), classifyQrContent(invitation) { target })
  }

  @Test
  fun desktopLinkIsTrimmedBeforeClassificationAndStored() {
    val invitation = "xrcp:/abc@192.168.1.5:55072#/?v=2&ssig=x&idsig=y"
    assertEquals(QrContent.DesktopLink(invitation), classifyQrContent("  $invitation\n", notSimplex))
  }

  @Test
  fun simplexAndUrlPayloadsAreUnaffectedByDesktopLinkDetection() {
    // Same stub pattern as simplexDetectorResultRoutesToSimpleXTarget: the
    // detector decides SimpleX-ness, here for a simplex:/ URI shape.
    val target = ConnectTarget.Link("simplex:/invitation#/?v=1&s=abc", SimplexLinkType.invitation, "Invitation")
    assertEquals(
      QrContent.SimpleXTarget(target),
      classifyQrContent("simplex:/invitation#/?v=1&s=abc") { target }
    )
    assertEquals(
      QrContent.Url("https://example.com/page?q=1"),
      classifyQrContent("https://example.com/page?q=1", notSimplex)
    )
    // Same-prefix-no-suffix payloads keep the documented Text fallback,
    // mirroring the "simplex:garbage" case.
    assertEquals(QrContent.Text("xrcp:garbage"), classifyQrContent("xrcp:garbage", notSimplex))
  }

  @Test
  fun desktopLinkShapeCheck() {
    assertTrue(isDesktopLink("xrcp:/abc@192.168.1.5:55072#/?v=2&ssig=x&idsig=y"))
    assertFalse(isDesktopLink("xrcp:garbage")) // scheme without the xrcp:/ body marker
    assertFalse(isDesktopLink("https://example.com/xrcp:/a")) // prefix must open the payload
    assertFalse(isDesktopLink("XRCP:/abc")) // case-sensitive, matching the wire format
  }

  @Test
  fun urlShapeCheck() {
    assertTrue(isHttpUrl("https://example.com"))
    assertTrue(isHttpUrl("HTTPS://EXAMPLE.COM/PATH"))
    assertTrue(isHttpUrl("http://localhost:5225"))
    assertTrue(isHttpUrl("https://simplex.chat/invitation#/?v=1"))
    assertFalse(isHttpUrl("example.com/page")) // no scheme
    assertFalse(isHttpUrl("ftp://example.com/file")) // wrong scheme
    assertFalse(isHttpUrl("http://")) // no host
    assertFalse(isHttpUrl("http://pathless")) // host without a dot and not localhost
    assertFalse(isHttpUrl("meet me at http central")) // words only
  }
}
