package chat.simplex.common.views.helpers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.theme.*

/**
 * Modifier creating an Apple Liquid Glass / frosted glassmorphism effect.
 * Combines translucent background tint, optional subtle shadow, specular gradient border, and clipping.
 */
@Composable
fun Modifier.liquidGlass(
  shape: Shape = CornerLarge,
  containerColor: Color? = null,
  borderAlpha: Float = 0.18f,
  elevation: Dp = 8.dp
): Modifier {
  val isDark = isInDarkTheme()
  val bg = containerColor ?: if (isDark) GlassFrostedDark else GlassFrostedLight
  val topHighlight = if (isDark) Color.White.copy(alpha = borderAlpha * 1.5f) else Color.White.copy(alpha = borderAlpha * 2f)
  val bottomHighlight = if (isDark) Color.White.copy(alpha = borderAlpha * 0.4f) else Color.Black.copy(alpha = borderAlpha * 0.3f)
  
  val borderBrush = Brush.verticalGradient(
    colors = listOf(topHighlight, bottomHighlight)
  )

  return this
    .shadow(
      elevation = elevation,
      shape = shape,
      ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.08f),
      spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.12f)
    )
    .clip(shape)
    .background(bg, shape)
    .border(
      border = BorderStroke(1.dp, borderBrush),
      shape = shape
    )
}

/**
 * Modern Glassmorphic Container Card with translucent frosted background and subtle specular border.
 */
@Composable
fun GlassmorphicCard(
  modifier: Modifier = Modifier,
  shape: Shape = GroupedCardShape,
  backgroundColor: Color? = null,
  elevation: Dp = 4.dp,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier.liquidGlass(
      shape = shape,
      containerColor = backgroundColor,
      elevation = elevation
    ),
    content = content
  )
}

/**
 * Floating Liquid Glass Dock for bottom navigation and call control overlays.
 */
@Composable
fun GlassmorphicFloatingDock(
  modifier: Modifier = Modifier,
  shape: Shape = FloatingDockShape,
  backgroundColor: Color? = null,
  content: @Composable RowScope.() -> Unit
) {
  Row(
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .liquidGlass(
        shape = shape,
        containerColor = backgroundColor,
        borderAlpha = 0.25f,
        elevation = 12.dp
      )
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
    content = content
  )
}

/**
 * Frosted circular action button (e.g. for hero profile and call screen).
 */
@Composable
fun GlassmorphicCircleButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  contentColor: Color = Color.White,
  backgroundColor: Color? = null,
  content: @Composable () -> Unit
) {
  val isDark = isInDarkTheme()
  val bg = backgroundColor ?: if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
  val borderBrush = Brush.verticalGradient(
    colors = listOf(
      Color.White.copy(alpha = if (isDark) 0.35f else 0.5f),
      Color.White.copy(alpha = if (isDark) 0.05f else 0.1f)
    )
  )

  Box(
    modifier = modifier
      .size(size)
      .bounceClick(onClick = onClick)
      .clip(CornerPill)
      .background(bg, CornerPill)
      .border(BorderStroke(1.dp, borderBrush), CornerPill),
    contentAlignment = Alignment.Center
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
      content()
    }
  }
}
