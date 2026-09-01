package chat.simplex.common.views.chat.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import chat.simplex.common.ui.theme.GlassTokens
import chat.simplex.common.ui.theme.isInDarkTheme

/**
 * Ambient background with soft glowing radial-gradient orbs.
 *
 * Creates a deep dark canvas with three colored orbs (Indigo, Violet, Cyan)
 * that provide the diffused light source refracted through glass surfaces.
 * In light mode, orb opacity is reduced and the base background is lighter.
 *
 * #99: `animated` defaults to false - the drift tweens write state every vsync
 * for a full-screen 3-radial-gradient redraw at display refresh rate for the
 * whole chat session, for a visually near-free +/-4% wobble. Opt in only if a
 * low-frequency gated animation (drawWithCache + <=10 Hz step) is built.
 * This is the outermost visual wrapper for the chat screen in glass mode.
 */
@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = isInDarkTheme()
    val baseBackground = if (isDark) GlassTokens.DarkBackground else Color(0xFFF0F2F5)
    val orbAlphaMultiplier = if (isDark) 1f else 0.5f

    val drift1: Float
    val drift2: Float
    if (animated) {
        val infiniteTransition = rememberInfiniteTransition()
        val a1 by infiniteTransition.animateFloat(
            initialValue = -0.04f,
            targetValue = 0.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(9000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val a2 by infiniteTransition.animateFloat(
            initialValue = 0.03f,
            targetValue = -0.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        drift1 = a1
        drift2 = a2
    } else {
        drift1 = 0f
        drift2 = 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Indigo / Electric Blue - top left (gently drifting)
            val c1 = Offset(width * (0.15f + drift1), height * (0.12f + drift2))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTokens.OrbIndigo.copy(alpha = GlassTokens.OrbIndigoAlpha * orbAlphaMultiplier),
                        Color.Transparent
                    ),
                    center = c1,
                    radius = width * 0.75f
                ),
                center = c1,
                radius = width * 0.75f
            )

            // Orb 2: Violet / Purple - center right
            val c2 = Offset(width * (0.85f - drift2), height * (0.45f + drift1))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTokens.OrbViolet.copy(alpha = GlassTokens.OrbVioletAlpha * orbAlphaMultiplier),
                        Color.Transparent
                    ),
                    center = c2,
                    radius = width * 0.65f
                ),
                center = c2,
                radius = width * 0.65f
            )

            // Orb 3: Cyan / Emerald - bottom left
            val c3 = Offset(width * (0.25f + drift2), height * (0.85f - drift1))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTokens.OrbCyan.copy(alpha = GlassTokens.OrbCyanAlpha * orbAlphaMultiplier),
                        Color.Transparent
                    ),
                    center = c3,
                    radius = width * 0.7f
                ),
                center = c3,
                radius = width * 0.7f
            )
        }
        content()
    }
}
