package chat.simplex.app

import androidx.compose.ui.geometry.Offset
import chat.simplex.common.views.ux.components.nextRevealTarget
import chat.simplex.common.views.ux.components.resolveRevealOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Regression for #14: trigger() used to set isAnimating without a re-entry guard and
// without resetting it on scope cancellation, leaving the reveal overlay stuck.
// The trigger decisions are extracted as pure functions and pinned here.
class ThemeAnimationStateTest {

  @Test
  fun idleRevealTogglesCurrentDarkness() {
    assertEquals(false, nextRevealTarget(isAnimating = false, currentIsDark = true))
    assertEquals(true, nextRevealTarget(isAnimating = false, currentIsDark = false))
  }

  @Test
  fun runningRevealIgnoresNewTriggers() {
    assertNull(nextRevealTarget(isAnimating = true, currentIsDark = false))
    assertNull(nextRevealTarget(isAnimating = true, currentIsDark = true))
  }

  @Test
  fun nullOriginFallsBackToDefault() {
    assertEquals(Offset(1000f, 150f), resolveRevealOrigin(null))
  }

  @Test
  fun providedOriginIsUsedAsIs() {
    assertEquals(Offset(42f, 7f), resolveRevealOrigin(Offset(42f, 7f)))
  }
}
