package chat.simplex.common.views.ux.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.theme.AmberGold

// Center scan reticule: four L-shaped gold corner brackets in the Luxury
// Mineral style (thin round-capped strokes, no heavy frame around the view).
//
// `pulse` drives the one-shot detection pulse: it animates 0 -> 1 each time a
// NEW code enters the frame and stays at 1 (idle, no pulse ring) while the
// same code remains detected. The pulse is never looped for a code that is
// already on screen.
@Composable
fun CameraReticule(pulse: Float, modifier: Modifier = Modifier) {
  Canvas(modifier.size(250.dp)) {
    val stroke = 3.dp.toPx()
    val arm = 34.dp.toPx()
    val inset = stroke / 2f
    val w = size.width
    val h = size.height
    val color = AmberGold.copy(alpha = 0.9f)

    fun bracket(x: Float, y: Float, dx: Float, dy: Float) {
      drawLine(color, Offset(x, y), Offset(x + dx * arm, y), stroke, StrokeCap.Round)
      drawLine(color, Offset(x, y), Offset(x, y + dy * arm), stroke, StrokeCap.Round)
    }
    bracket(inset, inset, 1f, 1f)
    bracket(w - inset, inset, -1f, 1f)
    bracket(inset, h - inset, 1f, -1f)
    bracket(w - inset, h - inset, -1f, -1f)

    if (pulse in 0f..0.999f) {
      drawCircle(
        AmberGold.copy(alpha = (1f - pulse) * 0.55f),
        radius = w / 2f + pulse * 36.dp.toPx(),
        style = Stroke(2.dp.toPx())
      )
    }
  }
}
