package chat.simplex.common.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism 2026 surface modifier.
 *
 * Applies crisp frosted glass aesthetics with high contrast and zero content blur:
 * 1. Smooth clipping to [shape].
 * 2. Vertical gradient translucent background with rich contrast and sharp readability.
 * 3. Shimmering specular border via a linear gradient from [borderColor] to subtle accent.
 *
 * #99: the two gradient brushes are remember-cached per color - this modifier sits on
 * the chat send bar and used to rebuild shape + brushes on every keystroke recomposition.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: Dp = 1.dp
): Modifier {
    val backgroundBrush = remember(backgroundColor) {
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = (backgroundColor.alpha * 0.88f).coerceAtMost(1f))
            )
        )
    }
    val borderBrush = remember(borderColor) {
        Brush.linearGradient(
            colors = listOf(
                borderColor,
                borderColor.copy(alpha = 0.15f)
            )
        )
    }
    return this
        .clip(shape)
        .background(brush = backgroundBrush, shape = shape)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}
