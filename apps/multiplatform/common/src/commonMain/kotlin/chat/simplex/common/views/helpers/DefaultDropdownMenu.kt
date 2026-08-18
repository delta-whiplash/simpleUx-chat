package chat.simplex.common.views.helpers

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
          else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)))
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            if (isDark) listOf(Color(0x45FFFFFF), Color(0x10FFFFFF))
            else listOf(Color(0x250F172A), Color(0x0C0F172A))
          ),
          shape = menuShape
        )
        .padding(vertical = 4.dp),
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
          else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)))
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            if (isDark) listOf(Color(0x45FFFFFF), Color(0x10FFFFFF))
            else listOf(Color(0x250F172A), Color(0x0C0F172A))
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