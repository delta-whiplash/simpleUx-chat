package chat.simplex.common.views.ux.camera

import chat.simplex.common.views.newchat.ConnectTarget
import chat.simplex.common.views.newchat.strConnectTarget

// Universal routing for anything the quick camera scans. Previously the sheet
// silently dropped every QR that was not a SimpleX link (the decoded content
// vanished with no confirmation card); every scanned payload now surfaces as
// one of these three kinds and gets a visible card.
//
// Pure commonMain with no platform imports: URL detection is a plain shape
// check, so the classifier is desktop-testable.
sealed class QrContent {
  data class SimpleXTarget(val target: ConnectTarget) : QrContent()
  data class Url(val url: String) : QrContent()
  data class Text(val text: String) : QrContent()
}

/**
 * Classifies a raw QR payload. SimpleX detection wins over URL detection, so
 * SimpleX invitation/contact/group links distributed as web URLs still route
 * to the in-app connect flow instead of the browser.
 *
 * `simplexDetector` defaults to the production SimpleX-link detection
 * (`strConnectTarget`, whose markdown parsing runs on the Haskell core via
 * JNI). It is an injectable seam because desktopTest runs without the native
 * library: unit tests stub the detector instead of loading the core.
 */
fun classifyQrContent(
  raw: String,
  simplexDetector: (String) -> ConnectTarget? = ::strConnectTarget
): QrContent {
  val trimmed = raw.trim()
  // Empty/blank payloads classify as empty Text. The camera analyzer never
  // reports one in practice, but the behavior is defined and total.
  if (trimmed.isEmpty()) return QrContent.Text(trimmed)
  val target = simplexDetector(trimmed)
  if (target != null) return QrContent.SimpleXTarget(target)
  if (isHttpUrl(trimmed)) return QrContent.Url(trimmed)
  return QrContent.Text(trimmed)
}

// Pure-Kotlin "sane URL" shape: an http(s) scheme and a non-empty host with a
// dot (or localhost; a trailing :port is not part of the host). Anything else
// falls through to Text, so a QR that happens to contain ordinary words never
// gets sent to the browser.
fun isHttpUrl(s: String): Boolean {
  val lower = s.lowercase()
  if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
  val authority = s.substringAfter("://")
  val host = authority.takeWhile { it != '/' && it != '?' && it != '#' }.substringBefore(':')
  return host.isNotEmpty() && (host.contains('.') || host.equals("localhost", ignoreCase = true))
}
