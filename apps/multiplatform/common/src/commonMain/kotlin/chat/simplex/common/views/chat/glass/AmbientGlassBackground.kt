package chat.simplex.common.views.chat.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
 * Features subtle floating & breathing animation for dynamic mineral luminescence.
 * This is the outermost visual wrapper for the chat screen in glass mode.
 *
 * In light mode, orb opacity is reduced and the base background is lighter.
 */
@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = isInDarkTheme()
    val baseBackground = if (isDark) GlassTokens.DarkBackground else Color(0xFFF0F2F5)
    val orbAlphaMultiplier = if (isDark) 1f else 0.5f

    val infiniteTransition = rememberInfiniteTransition()
    val drift1 by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -0.04f,
            targetValue = 0.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(9000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val drift2 by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.03f,
            targetValue = -0.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(0f) }
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
