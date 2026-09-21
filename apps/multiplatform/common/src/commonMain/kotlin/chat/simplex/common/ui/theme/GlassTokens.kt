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
    // #175: the glass-mode canvas is the mineral obsidian canvas
    val DarkBackground = MineralCanvasDark

    // ── Ambient orb colors (static radial glows behind glass) ───────
    // #175: mineral glows - steel, champagne and a whisper of azure -
    // replacing the indigo/violet/cyan trio. Kept names, new values.
    val OrbIndigo = Color(0xFF3D5A80)
    val OrbViolet = ChampagneGold
    val OrbCyan = Sky400

    const val OrbIndigoAlpha = 0.16f
    const val OrbVioletAlpha = 0.10f
    const val OrbCyanAlpha = 0.08f

    // ── Sent bubble (sapphire steel, #175) ──────────────────────────
    val SentBubblePrimary = MineralOutTopDark
    val SentBubbleAccent = Color(0xFF7FA6D9)
    const val SentBubbleAlpha = 0.88f
    const val SentBorderAlpha = 0.45f

    // ── Received bubble ─────────────────────────────────────────────
    val ReceivedBubbleColor = MineralInBubbleDark
    const val ReceivedBubbleAlpha = 0.88f
    const val ReceivedBorderAlpha = 0.25f

    // ── Security pill (emerald = security truth, #175) ──────────────
    val SecurityPillBg = Color(0xFF122A20)
    val SecurityPillBorder = Color(0x6610B981)
    val SecurityPillIcon = Color(0xFF6EE7B7)
    val SecurityPillText = Color(0xFFD1FAE5)

    // ── Chat list surfaces ─────────────────────────────────────────
    val ChatListCardBorderLight = Color(0x140F172A)
    val FilterChipInactiveTextLight = Slate600
    val SearchBarBgLight = Slate100
}
