package chat.simplex.common.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Glassmorphism 2026 design tokens.
 *
 * Centralizes all color constants, alpha values, and gradient parameters
 * used by the glass surface modifier and glass-styled components.
 */
object GlassTokens {

    // ── Deep background ──────────────────────────────────────────────
    val DarkBackground = Color(0xFF07090E)

    // ── Surface alpha levels ─────────────────────────────────────────
    const val SurfaceAlphaDark = 0.08f
    const val SurfaceAlphaLight = 0.12f
    const val BorderAlphaDark = 0.18f
    const val BorderAlphaLight = 0.10f

    // ── Ambient orb colors (radial gradients behind glass) ──────────
    val OrbIndigo = Color(0xFF3B82F6)
    val OrbViolet = Color(0xFF8B5CF6)
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
    val ReceivedBubbleColor = Color(0xFF1E293B)
    const val ReceivedBubbleAlpha = 0.88f
    const val ReceivedBorderAlpha = 0.25f

    // ── Security pill ───────────────────────────────────────────────
    val SecurityPillBg = Color(0xFF1E3A8A)
    val SecurityPillBorder = Color(0xFF60A5FA)
    val SecurityPillIcon = Color(0xFF93C5FD)
    val SecurityPillText = Color(0xFFE2E8F0)

    // ── Voice player ────────────────────────────────────────────────
    val VoicePlayButtonBg = Color(0xFF2563EB)
    val VoiceBarActive = Color(0xFF60A5FA)
    val VoiceBarInactive = Color.White
    const val VoiceBarInactiveAlpha = 0.35f
    val VoiceSpeedText = Color(0xFF93C5FD)

    // ── Input bar ───────────────────────────────────────────────────
    val SendButtonGradientStart = Color(0xFF2563EB)
    val SendButtonGradientEnd = Color(0xFF7C3AED)
    val CursorColor = Color(0xFF60A5FA)
    val PlaceholderColor = Color.White.copy(alpha = 0.65f)

    // ── Status indicator ────────────────────────────────────────────
    val OnlineGreen = Color(0xFF10B981)
    val ReadReceipt = Color(0xFF93C5FD)

    // ── Sender name accent ──────────────────────────────────────────
    val SenderNameAccent = Color(0xFF60A5FA)

    // ── Blur radius ─────────────────────────────────────────────────
    const val DefaultBlurRadius = 40f
    const val LightBlurRadius = 24f

    /**
     * Returns the appropriate glass surface background color
     * based on the current theme mode.
     */
    @Composable
    fun surfaceColor(): Color {
        return if (isInDarkTheme()) {
            Color.White.copy(alpha = SurfaceAlphaDark)
        } else {
            Color.Black.copy(alpha = SurfaceAlphaLight)
        }
    }

    /**
     * Returns the appropriate glass border color
     * based on the current theme mode.
     */
    @Composable
    fun borderColor(): Color {
        return if (isInDarkTheme()) {
            Color.White.copy(alpha = BorderAlphaDark)
        } else {
            Color.Black.copy(alpha = BorderAlphaLight)
        }
    }

    // ── Chat List Tokens ───────────────────────────────────────────
    val ChatListCardBgDark = Color(0x66182232)
    val ChatListCardBgLight = Color(0x88FFFFFF)
    val ChatListCardBorderDark = Color(0x2EFFFFFF)
    val ChatListCardBorderLight = Color(0x1F000000)
    val ChatListCardSelectedDark = Color(0x991E3A5F)

    val FilterChipActiveBg = Color(0x2900E5FF)
    val FilterChipActiveBorder = Color(0x6600E5FF)
    val FilterChipActiveText = Color(0xFF38BDF8)
    val FilterChipInactiveBg = Color(0x331E293B)
    val FilterChipInactiveBorder = Color(0x22FFFFFF)
    val FilterChipInactiveText = Color(0xFF94A3B8)

    val SearchBarBgDark = Color(0x661E293B)
    val SearchBarBorderDark = Color(0x33FFFFFF)

    val UnreadBadgeStart = Color(0xFF00E5FF)
    val UnreadBadgeEnd = Color(0xFF0088FF)

    /**
     * Returns the blur radius appropriate for the current theme.
     */
    @Composable
    fun blurRadius(): Float {
        return if (isInDarkTheme()) DefaultBlurRadius else LightBlurRadius
    }
}
