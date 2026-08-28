package chat.simplex.app

import chat.simplex.common.views.ux.components.nextPlaybackSpeed
import kotlin.test.Test
import kotlin.test.assertEquals

// Regression for #13: the speed button used to cycle local UI state that never reached
// the player. The cycle itself is a pure state machine so it can be pinned by tests.
class PlaybackSpeedCycleTest {

  @Test
  fun normalSpeedCyclesToOneAndAHalf() {
    assertEquals(1.5f, nextPlaybackSpeed(1.0f))
  }

  @Test
  fun oneAndAHalfCyclesToDouble() {
    assertEquals(2.0f, nextPlaybackSpeed(1.5f))
  }

  @Test
  fun doubleCyclesBackToNormal() {
    assertEquals(1.0f, nextPlaybackSpeed(2.0f))
  }

  @Test
  fun unexpectedSpeedRestartsCycleAtNormal() {
    assertEquals(1.0f, nextPlaybackSpeed(1.25f))
  }
}
