package chat.simplex.common.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Glassmorphism 2026 design tokens.
 *
 * Centralizes the glass colors actually in use. Accent/base values reference
 * the canonical tokens in Color.kt instead of re-declaring hex literals (#16,
 * #106).
 */
object GlassTokens {

    // ── Deep background ──────────────────────────────────────────────
    val DarkBackground = Color(0xFF07090E)

    // ── Ambient orb colors (radial gradients behind glass) ──────────
    val OrbIndigo = Blue500
    val OrbViolet = VioletPurple
    val OrbCyan = Color(0xFF06B6D4)

    const val OrbIndigoAlpha = 0.28f
    const val OrbVioletAlpha = 0.22f
    const val OrbCyanAlpha = 0.18f

    // ── Sent bubble ─────────────────────────────────────────────────
    val SentBubblePrimary = Color(0xFF1E40AF)
    val SentBubbleAccent = Color(0xFF60A5FA)
    const val SentBubbleAlpha = 0.88f
    const val SentBorderAlpha = 0.45f

    // ── Received bubble ─────────────────────────────────────────────
    val ReceivedBubbleColor = Slate800
    const val ReceivedBubbleAlpha = 0.88f
    const val ReceivedBorderAlpha = 0.25f

    // ── Security pill ───────────────────────────────────────────────
    val SecurityPillBg = Color(0xFF1E3A8A)
    val SecurityPillBorder = Color(0xFF60A5FA)
    val SecurityPillIcon = Color(0xFF93C5FD)
    val SecurityPillText = Color(0xFFE2E8F0)

    // ── Chat list surfaces ─────────────────────────────────────────
    val ChatListCardBorderLight = Color(0x140F172A)
    val FilterChipInactiveTextLight = Slate600
    val SearchBarBgLight = Slate100
}
