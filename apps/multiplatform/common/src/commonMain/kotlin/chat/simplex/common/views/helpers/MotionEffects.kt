package chat.simplex.common.views.helpers

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Spring physics specs for tactile Material You and iOS-like micro-interactions.
 */
val SnappySpringSpec = spring<Float>(
  dampingRatio = Spring.DampingRatioMediumBouncy,
  stiffness = Spring.StiffnessLow
)

val GentleSpringSpec = spring<Float>(
  dampingRatio = Spring.DampingRatioNoBouncy,
  stiffness = Spring.StiffnessMedium
)

private enum class PressState { Pressed, Idle }

/**
 * Modifier that applies a satisfying tactile scale-down spring bounce effect on press.
 */
fun Modifier.bounceClick(
  scaleDown: Float = 0.95f,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null
): Modifier = composed {
  var pressState by remember { mutableStateOf(PressState.Idle) }
  val scale by animateFloatAsState(
    targetValue = if (pressState == PressState.Pressed) scaleDown else 1f,
    animationSpec = SnappySpringSpec,
    label = "bounceScale"
  )

  this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .pointerInput(pressState, enabled) {
      if (!enabled) return@pointerInput
      awaitPointerEventScope {
        pressState = if (pressState == PressState.Pressed) {
          waitForUpOrCancellation()
          PressState.Idle
        } else {
          awaitFirstDown(false)
          PressState.Pressed
        }
      }
    }
    .then(
      if (onClick != null && enabled) {
        Modifier.clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onClick
        )
      } else Modifier
    )
}
