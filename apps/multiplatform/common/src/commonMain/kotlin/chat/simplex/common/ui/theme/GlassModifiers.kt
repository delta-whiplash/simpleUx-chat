package chat.simplex.common.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = if (isInDarkTheme()) Color(0xDD141D2B) else Color(0xEEF8FAFC),
    borderColor: Color = if (isInDarkTheme()) Color(0x38FFFFFF) else Color(0x20000000),
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp
): Modifier {
    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = (backgroundColor.alpha * 0.88f).coerceAtMost(1f))
                )
            ),
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.15f)
                )
            ),
            shape = shape
        )
}
