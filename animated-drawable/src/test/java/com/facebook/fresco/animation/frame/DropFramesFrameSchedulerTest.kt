/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.frame

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.IntRange
import com.facebook.fresco.animation.backend.AnimationBackend
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

/** Tests [DropFramesFrameScheduler]. */
class DropFramesFrameSchedulerTest {

  private lateinit var dummyAnimationBackend: DummyAnimationBackend
  private lateinit var frameScheduler: DropFramesFrameScheduler

  @Before
  fun setUp() {
    dummyAnimationBackend = DummyAnimationBackend(5)
    frameScheduler = DropFramesFrameScheduler(dummyAnimationBackend)
  }

  @Test
  fun testGetFrameNumberToRender() {
    assertThat(frameScheduler.getFrameNumberToRender(0, -1)).isEqualTo(0)
    assertThat(frameScheduler.getFrameNumberToRender(50, -1)).isEqualTo(0)
    assertThat(frameScheduler.getFrameNumberToRender(100, -1)).isEqualTo(1)
    assertThat(frameScheduler.getFrameNumberToRender(499, -1)).isEqualTo(4)
    assertThat(frameScheduler.getFrameNumberToRender(500, -1)).isEqualTo(0)
    assertThat(frameScheduler.getFrameNumberToRender(600, -1)).isEqualTo(1)
    assertThat(frameScheduler.getFrameNumberToRender(601, -1)).isEqualTo(1)
  }

  @Test
  fun testGetLoopDurationMs() {
    assertThat(frameScheduler.loopDurationMs).isEqualTo(500)
  }

  @Test
  fun testGetTargetRenderTimeMs() {
    assertThat(frameScheduler.getTargetRenderTimeMs(0)).isEqualTo(0)
    assertThat(frameScheduler.getTargetRenderTimeMs(1)).isEqualTo(100)
    assertThat(frameScheduler.getTargetRenderTimeMs(2)).isEqualTo(200)
    assertThat(frameScheduler.getTargetRenderTimeMs(3)).isEqualTo(300)
    assertThat(frameScheduler.getTargetRenderTimeMs(4)).isEqualTo(400)
  }

  @Test
  fun testGetTargetRenderTimeForNextFrameMs() {
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(0)).isEqualTo(100)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(1)).isEqualTo(100)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(50)).isEqualTo(100)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(100)).isEqualTo(200)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(170)).isEqualTo(200)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(460)).isEqualTo(500)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(499)).isEqualTo(500)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(500)).isEqualTo(600)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(501)).isEqualTo(600)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(510)).isEqualTo(600)
  }

  @Test
  fun testGetTargetRenderTimeForNextFrameMsWhenAnimationOver() {
    val animationDurationMs = dummyAnimationBackend.getAnimationDurationMs()

    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs - 1))
        .isEqualTo(animationDurationMs)
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong())
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs + 1))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong())
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs + 100))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong())
    assertThat(frameScheduler.getTargetRenderTimeForNextFrameMs(animationDurationMs * 100))
        .isEqualTo(FrameScheduler.NO_NEXT_TARGET_RENDER_TIME.toLong())
  }

  @Test
  fun testIsInfiniteAnimation() {
    assertThat(frameScheduler.isInfiniteAnimation).isFalse()
  }

  @Test
  fun testLoopCount() {
    val animationDurationMs = dummyAnimationBackend.getAnimationDurationMs()
    val lastFrameNumber = dummyAnimationBackend.frameCount - 1

    assertThat(frameScheduler.getFrameNumberToRender(animationDurationMs, -1))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE)

    assertThat(frameScheduler.getFrameNumberToRender(animationDurationMs + 1, -1))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE)

    assertThat(
            frameScheduler.getFrameNumberToRender(
                animationDurationMs + dummyAnimationBackend.getFrameDurationMs(lastFrameNumber),
                -1,
            ))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE)

    assertThat(
            frameScheduler.getFrameNumberToRender(
                animationDurationMs +
                    dummyAnimationBackend.getFrameDurationMs(lastFrameNumber) +
                    100,
                -1,
            ))
        .isEqualTo(FrameScheduler.FRAME_NUMBER_DONE)
  }

  @Test
  fun testGetFrameNumberWithinLoop() {
    assertThat(frameScheduler.getFrameNumberWithinLoop(0)).isEqualTo(0)
    assertThat(frameScheduler.getFrameNumberWithinLoop(1)).isEqualTo(0)
    assertThat(frameScheduler.getFrameNumberWithinLoop(99)).isEqualTo(0)
    assertThat(frameScheduler.getFrameNumberWithinLoop(100)).isEqualTo(1)
    assertThat(frameScheduler.getFrameNumberWithinLoop(101)).isEqualTo(1)
    assertThat(frameScheduler.getFrameNumberWithinLoop(250)).isEqualTo(2)
    assertThat(frameScheduler.getFrameNumberWithinLoop(499)).isEqualTo(4)
  }

  @Test
  fun testGetFrameNumberToRender_whenNoFrames_thenReturnFirstFrame() {
    val backend = DummyAnimationBackend(0)
    val frameScheduler = DropFramesFrameScheduler(backend)

    assertThat(frameScheduler.loopDurationMs).isEqualTo(0)
    assertThat(frameScheduler.getFrameNumberToRender(0, 0)).isEqualTo(0)
  }

  private class DummyAnimationBackend(private val frameCount: Int) : AnimationBackend {

    override fun getLoopDurationMs(): Int {
      var loopDuration = 0L
      for (i in 0 until frameCount) {
        loopDuration += getFrameDurationMs(i)
      }
      return loopDuration.toInt()
    }

    override fun width(): Int = intrinsicWidth

    override fun height(): Int = intrinsicHeight

    fun getAnimationDurationMs(): Long = getLoopDurationMs().toLong() * getLoopCount()

    override fun getFrameCount(): Int = frameCount

    override fun getFrameDurationMs(frameNumber: Int): Int = 100

    override fun getLoopCount(): Int = 7

    override fun drawFrame(parent: Drawable, canvas: Canvas, frameNumber: Int): Boolean = false

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun setBounds(bounds: Rect) = Unit

    override fun getIntrinsicWidth(): Int = AnimationBackend.INTRINSIC_DIMENSION_UNSET

    override fun getIntrinsicHeight(): Int = AnimationBackend.INTRINSIC_DIMENSION_UNSET

    override fun getSizeInBytes(): Int = 0

    override fun clear() = Unit

    override fun preloadAnimation() = Unit

    override fun setAnimationListener(listener: AnimationBackend.Listener?) = Unit
  }
}
