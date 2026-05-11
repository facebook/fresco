/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.backend.AnimationInformation
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimatedDrawable2CircuitBreakerTest {

  @Mock private lateinit var animationBackend: AnimationBackend
  @Mock private lateinit var canvas: Canvas
  @Mock private lateinit var callback: Drawable.Callback
  @Mock private lateinit var animationListener: AnimationListener

  private lateinit var drawable: AnimatedDrawable2
  private lateinit var closeable: AutoCloseable

  @Before
  fun setup() {
    closeable = MockitoAnnotations.openMocks(this)

    whenever(animationBackend.frameCount).thenReturn(10)
    whenever(animationBackend.loopCount).thenReturn(AnimationInformation.LOOP_COUNT_INFINITE)
    whenever(animationBackend.loopDurationMs).thenReturn(1000)
    whenever(animationBackend.getFrameDurationMs(any())).thenReturn(100)

    drawable = AnimatedDrawable2(animationBackend)
    drawable.callback = callback
    drawable.setAnimationListener(animationListener)
    drawable.setBounds(0, 0, 100, 100)
    drawable.start()
  }

  @After
  fun tearDown() {
    closeable.close()
  }

  @Test
  fun `circuit breaker trips after consecutive frame drops exceed threshold`() {
    // All frames fail to render
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(false)

    // Draw more than the circuit breaker threshold (default 30)
    repeat(31) { drawable.draw(canvas) }

    // Animation should have stopped
    assertThat(drawable.isRunning).isFalse()
  }

  @Test
  fun `successful frame resets consecutive drop counter`() {
    // First 29 frames fail (just under threshold)
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(false)
    repeat(29) { drawable.draw(canvas) }

    // One successful frame resets the counter
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(true)
    drawable.draw(canvas)

    // Another 29 failures should not trip the breaker (counter was reset)
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(false)
    repeat(29) { drawable.draw(canvas) }

    // Should still be running since we never hit 30 consecutive failures
    assertThat(drawable.isRunning).isTrue()
  }

  @Test
  fun `circuit breaker notifies animation listener on stop`() {
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(false)

    repeat(31) { drawable.draw(canvas) }

    // onAnimationStop should have been called when the circuit breaker tripped
    verify(animationListener, org.mockito.kotlin.atLeastOnce()).onAnimationStop(drawable)
  }

  @Test
  fun `start resets circuit breaker after it trips`() {
    // Trip the circuit breaker
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(false)
    repeat(31) { drawable.draw(canvas) }
    assertThat(drawable.isRunning).isFalse()

    // Restart the animation (simulates view becoming visible again)
    drawable.start()
    assertThat(drawable.isRunning).isTrue()

    // It should be able to draw again if frames now succeed
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(true)
    drawable.draw(canvas)
    assertThat(drawable.isRunning).isTrue()
  }

  @Test
  fun `circuit breaker does not trip when frames succeed`() {
    // All frames render successfully
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(true)

    repeat(100) { drawable.draw(canvas) }

    // Should still be running
    assertThat(drawable.isRunning).isTrue()
  }

  @Test
  fun `animation backend clear is not called when circuit breaker trips`() {
    // Trip the circuit breaker
    whenever(animationBackend.drawFrame(any(), any(), any())).thenReturn(false)
    repeat(31) { drawable.draw(canvas) }

    // clear() should NOT be called — we want to preserve the last drawn frame
    verify(animationBackend, never()).clear()
  }
}
