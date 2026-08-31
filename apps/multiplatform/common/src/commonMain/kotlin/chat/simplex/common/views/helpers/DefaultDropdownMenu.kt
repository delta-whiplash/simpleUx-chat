package chat.simplex.common.views.helpers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.theme.isInDarkTheme

@Composable
fun DefaultDropdownMenu(
  showMenu: MutableState<Boolean>,
  modifier: Modifier = Modifier,
  offset: DpOffset = DpOffset(0.dp, 0.dp),
  onClosed: State<() -> Unit> = remember { mutableStateOf({}) },
  dropdownMenuItems: (@Composable () -> Unit)?
) {
  val isDark = isInDarkTheme()
  val menuShape = RoundedCornerShape(16.dp)
  
  // Scale-in animation for visual continuity
  val scaleAnim = remember { Animatable(0.85f) }
  val alphaAnim = remember { Animatable(0f) }
  
  LaunchedEffect(showMenu.value) {
    if (showMenu.value) {
      scaleAnim.snapTo(0.85f)
      alphaAnim.snapTo(0f)
      scaleAnim.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
      )
      alphaAnim.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f)
      )
    }
  }

  MaterialTheme(
    shapes = MaterialTheme.shapes.copy(medium = menuShape)
  ) {
    DropdownMenu(
      expanded = showMenu.value,
      onDismissRequest = { showMenu.value = false },
      modifier = modifier
        .width(220.dp)
        .clip(menuShape)
        .background(
          if (isDark) Brush.verticalGradient(listOf(Color(0xFF1E2533), Color(0xFF131720)))
          else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            if (isDark) listOf(Color(0x45FFFFFF), Color(0x10FFFFFF))
            else listOf(Color(0x1E0F172A), Color(0x0A0F172A))
          ),
          shape = menuShape
        )
        .padding(vertical = 4.dp)
        .graphicsLayer {
          scaleX = scaleAnim.value
          scaleY = scaleAnim.value
          alpha = alphaAnim.value
          transformOrigin = TransformOrigin(0.5f, 0.5f)
        },
      offset = offset,
    ) {
      dropdownMenuItems?.invoke()
      DisposableEffect(Unit) {
        onDispose {
          onClosed.value()
        }
      }
    }
  }
}

@Composable
fun ExposedDropdownMenuBoxScope.DefaultExposedDropdownMenu(
  expanded: MutableState<Boolean>,
  modifier: Modifier = Modifier,
  dropdownMenuItems: (@Composable () -> Unit)?
) {
  val isDark = isInDarkTheme()
  val menuShape = RoundedCornerShape(18.dp)

  MaterialTheme(
    shapes = MaterialTheme.shapes.copy(medium = menuShape)
  ) {
    ExposedDropdownMenu(
      modifier = Modifier
        .widthIn(min = 200.dp)
        .clip(menuShape)
        .background(
          if (isDark) Brush.verticalGradient(listOf(Color(0xFF1E2533), Color(0xFF131720)))
          else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            if (isDark) listOf(Color(0x45FFFFFF), Color(0x10FFFFFF))
            else listOf(Color(0x1E0F172A), Color(0x0A0F172A))
          ),
          shape = menuShape
        )
        .then(modifier),
      expanded = expanded.value,
      onDismissRequest = {
        expanded.value = false
      }
    ) {
      dropdownMenuItems?.invoke()
    }
  }
}