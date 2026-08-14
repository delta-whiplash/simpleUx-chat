package chat.simplex.common.views.chat.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
 * This is the outermost visual wrapper for the chat screen in glass mode.
 *
 * In light mode, orb opacity is reduced and the base background is lighter.
 */
@Composable
fun AmbientGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isInDarkTheme()
    val baseBackground = if (isDark) GlassTokens.DarkBackground else Color(0xFFF0F2F5)
    val orbAlphaMultiplier = if (isDark) 1f else 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Indigo / Electric Blue — top left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTokens.OrbIndigo.copy(alpha = GlassTokens.OrbIndigoAlpha * orbAlphaMultiplier),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.12f),
                    radius = width * 0.75f
                ),
                center = Offset(width * 0.15f, height * 0.12f),
                radius = width * 0.75f
            )

            // Orb 2: Violet / Purple — center right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTokens.OrbViolet.copy(alpha = GlassTokens.OrbVioletAlpha * orbAlphaMultiplier),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.45f),
                    radius = width * 0.65f
                ),
                center = Offset(width * 0.85f, height * 0.45f),
                radius = width * 0.65f
            )

            // Orb 3: Cyan / Emerald — bottom left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTokens.OrbCyan.copy(alpha = GlassTokens.OrbCyanAlpha * orbAlphaMultiplier),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.25f, height * 0.85f),
                    radius = width * 0.7f
                ),
                center = Offset(width * 0.25f, height * 0.85f),
                radius = width * 0.7f
            )
        }
        content()
    }
}
