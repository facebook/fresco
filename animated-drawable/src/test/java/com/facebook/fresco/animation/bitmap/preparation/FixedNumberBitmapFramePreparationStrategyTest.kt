/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import kotlin.Unit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests [FixedNumberBitmapFramePreparationStrategy]. */
@RunWith(RobolectricTestRunner::class)
class FixedNumberBitmapFramePreparationStrategyTest {

  companion object {
    const val NUMBER_OF_FRAMES_TO_PREPARE = 3
    const val FRAME_COUNT = 10
  }

  @Mock lateinit var animationBackend: AnimationBackend
  @Mock lateinit var bitmapFramePreparer: BitmapFramePreparer
  @Mock lateinit var bitmapFrameCache: BitmapFrameCache
  @Mock lateinit var onAnimationLoaded: () -> Unit

  private lateinit var bitmapFramePreparationStrategy: BitmapFramePreparationStrategy

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    bitmapFramePreparationStrategy =
        FixedNumberBitmapFramePreparationStrategy(NUMBER_OF_FRAMES_TO_PREPARE)
    whenever(animationBackend.frameCount).thenReturn(FRAME_COUNT)
    whenever(bitmapFramePreparer.prepareFrame(eq(bitmapFrameCache), eq(animationBackend), any()))
        .thenReturn(true)
  }

  @Test
  fun testPrepareFrames_FromFirstFrame() {
    bitmapFramePreparationStrategy.prepareFrames(
        bitmapFramePreparer, bitmapFrameCache, animationBackend, 0, onAnimationLoaded)
    verifyPrepareCalledForFramesInOrder(1, 2, 3)
  }

  @Test
  fun testPrepareFrames_FromLastFrame() {
    bitmapFramePreparationStrategy.prepareFrames(
        bitmapFramePreparer, bitmapFrameCache, animationBackend, 9, onAnimationLoaded)
    verifyPrepareCalledForFramesInOrder(0, 1, 2)
  }

  @Test
  fun testPrepareFrames_ExactlyLastFrames() {
    bitmapFramePreparationStrategy.prepareFrames(
        bitmapFramePreparer, bitmapFrameCache, animationBackend, 6, onAnimationLoaded)
    verifyPrepareCalledForFramesInOrder(7, 8, 9)
  }

  @Test
  fun testPrepareFrames_FrameOverflow() {
    bitmapFramePreparationStrategy.prepareFrames(
        bitmapFramePreparer, bitmapFrameCache, animationBackend, 8, onAnimationLoaded)
    verifyPrepareCalledForFramesInOrder(9, 0, 1)
  }

  @Test
  fun testPrepareFrames_FromFirstFrame_WhenBitmapFramePreparerAlwaysFails() {
    whenever(bitmapFramePreparer.prepareFrame(eq(bitmapFrameCache), eq(animationBackend), any()))
        .thenReturn(false)
    bitmapFramePreparationStrategy.prepareFrames(
        bitmapFramePreparer, bitmapFrameCache, animationBackend, 0, onAnimationLoaded)
    verifyPrepareCalledForFramesInOrder(1)
  }

  @Test
  fun testPrepareFrames_FromFirstFrame_WhenBitmapFramePreparerFailsForSelectedFrames() {
    whenever(bitmapFramePreparer.prepareFrame(eq(bitmapFrameCache), eq(animationBackend), eq(2)))
        .thenReturn(false)
    whenever(bitmapFramePreparer.prepareFrame(eq(bitmapFrameCache), eq(animationBackend), eq(3)))
        .thenReturn(false)
    bitmapFramePreparationStrategy.prepareFrames(
        bitmapFramePreparer, bitmapFrameCache, animationBackend, 0, onAnimationLoaded)
    verifyPrepareCalledForFramesInOrder(1, 2)
  }

  @Test
  fun testPrepareFrames_onAnimationLoadedIsTrigger_WhenFramesAreLoaded() {
    bitmapFramePreparationStrategy.prepareFrames(
        bitmapFramePreparer, bitmapFrameCache, animationBackend, 0, onAnimationLoaded)

    verify(onAnimationLoaded).invoke()
  }

  private fun verifyPrepareCalledForFramesInOrder(vararg frameNumbers: Int) {
    val inOrderBitmapFramePreparer = inOrder(bitmapFramePreparer)
    for (frameNumber in frameNumbers) {
      inOrderBitmapFramePreparer
          .verify(bitmapFramePreparer)
          .prepareFrame(bitmapFrameCache, animationBackend, frameNumber)
    }
    inOrderBitmapFramePreparer.verifyNoMoreInteractions()
  }
}
