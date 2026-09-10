package com.example

import com.example.ui.home.components.PlayerDoubleTapFeedback
import com.example.ui.home.components.PlayerGestureType
import com.example.ui.home.components.PlayerSeekHudState
import com.example.ui.home.components.formatGestureTime
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying player gestures and helper logic.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun formatGestureTime_formatsCorrectly() {
    assertEquals("00:00", formatGestureTime(0L))
    assertEquals("00:10", formatGestureTime(10_000L))
    assertEquals("01:30", formatGestureTime(90_000L))
    assertEquals("1:05:30", formatGestureTime(3_930_000L))
  }

  @Test
  fun playerDoubleTapFeedback_initializesProperly() {
    val forward = PlayerDoubleTapFeedback(isRight = true)
    val rewind = PlayerDoubleTapFeedback(isRight = false)
    assertTrue(forward.isRight)
    assertFalse(rewind.isRight)
  }

  @Test
  fun playerSeekHudState_computesForwardStatus() {
    val forwardState = PlayerSeekHudState(targetMs = 45000L, durationMs = 120000L, deltaMs = 15000L, isForward = true)
    val rewindState = PlayerSeekHudState(targetMs = 15000L, durationMs = 120000L, deltaMs = -15000L, isForward = false)
    assertTrue(forwardState.isForward)
    assertFalse(rewindState.isForward)
    assertEquals(PlayerGestureType.SWIPE_SEEK, PlayerGestureType.valueOf("SWIPE_SEEK"))
  }
}
