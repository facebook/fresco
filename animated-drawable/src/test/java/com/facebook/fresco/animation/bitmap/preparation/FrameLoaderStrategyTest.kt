/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoader
import com.facebook.fresco.animation.bitmap.preparation.ondemandanimation.FrameLoaderFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameLoaderStrategyTest {

  private val bitmapFrameRenderer: BitmapFrameRenderer = mock()
  private val frameLoader: FrameLoader = mock()
  private val frameLoaderFactory: FrameLoaderFactory = mock {
    on { createBufferLoader(any(), any(), any()) } doReturn frameLoader
  }

  @Test
  fun prepareFrames_extremelyTallAnimation_preservesPositiveWidth() {
    val strategy = createStrategy(animationWidth = 1, animationHeight = 1000)

    strategy.prepareFrames(canvasWidth = 1, canvasHeight = 2, onAnimationLoaded = null)

    verify(frameLoader).prepareFrames(eq(1), eq(2), any())
  }

  @Test
  fun prepareFrames_extremelyWideAnimation_preservesPositiveHeight() {
    val strategy = createStrategy(animationWidth = 1000, animationHeight = 1)

    strategy.prepareFrames(canvasWidth = 2, canvasHeight = 1, onAnimationLoaded = null)

    verify(frameLoader).prepareFrames(eq(2), eq(1), any())
  }

  @Test
  fun prepareFrames_shortWideCanvas_fitsBothDimensions() {
    val strategy = createStrategy(animationWidth = 100, animationHeight = 100)

    strategy.prepareFrames(canvasWidth = 200, canvasHeight = 10, onAnimationLoaded = null)

    verify(frameLoader).prepareFrames(eq(10), eq(10), any())
  }

  @Test
  fun prepareFrames_tallCanvas_fitsBothDimensions() {
    val strategy = createStrategy(animationWidth = 1000, animationHeight = 100)

    strategy.prepareFrames(canvasWidth = 100, canvasHeight = 1000, onAnimationLoaded = null)

    verify(frameLoader).prepareFrames(eq(100), eq(10), any())
  }

  @Test
  fun getBitmapFrame_zeroCanvasDimension_doesNotLoadFrame() {
    val strategy = createStrategy(animationWidth = 1, animationHeight = 1000)

    strategy.getBitmapFrame(frameNumber = 0, canvasWidth = 0, canvasHeight = 2)

    verifyNoInteractions(frameLoader)
  }

  @Test
  fun getBitmapFrame_zeroAnimationDimension_doesNotLoadFrame() {
    val strategy = createStrategy(animationWidth = 0, animationHeight = 1000)

    strategy.getBitmapFrame(frameNumber = 0, canvasWidth = 2, canvasHeight = 2)

    verifyNoInteractions(frameLoader)
  }

  @Test
  fun onStop_withoutSource_retainsFramesForRestart() {
    val strategy = createStrategy(animationWidth = 100, animationHeight = 100, source = null)
    strategy.prepareFrames(canvasWidth = 100, canvasHeight = 100, onAnimationLoaded = null)

    strategy.onStop()

    verify(frameLoader).onStop()
    verify(frameLoader, org.mockito.Mockito.never()).clear()
  }

  @Test
  fun onStop_withoutSource_reusesTheSameLoaderOnRestart() {
    val factory = FrameLoaderFactory(
        platformBitmapFactory = mock(),
        maxFpsRender = 30,
        bufferLengthMilliseconds = 1000,
        enableSingleFrameRendering = true,
    )
    val strategy = createStrategy(
        animationWidth = 100,
        animationHeight = 100,
        source = null,
        factory = factory,
    )
    val loaderField = FrameLoaderStrategy::class.java.getDeclaredField("frameLoader")
    loaderField.isAccessible = true
    strategy.prepareFrames(canvasWidth = 100, canvasHeight = 100, onAnimationLoaded = null)
    val beforeStop = loaderField.get(strategy)

    strategy.onStop()
    strategy.prepareFrames(canvasWidth = 100, canvasHeight = 100, onAnimationLoaded = null)

    org.junit.Assert.assertSame(beforeStop, loaderField.get(strategy))
    val secondStrategy = createStrategy(
        animationWidth = 100,
        animationHeight = 100,
        source = null,
        factory = factory,
    )
    secondStrategy.prepareFrames(canvasWidth = 100, canvasHeight = 100, onAnimationLoaded = null)
    org.junit.Assert.assertNotSame(beforeStop, loaderField.get(secondStrategy))
  }

  @Test
  fun onStop_withoutSource_expiredLoaderIsClearedAndNotReused() {
    val factory = FrameLoaderFactory(
        platformBitmapFactory = mock(),
        maxFpsRender = 30,
        bufferLengthMilliseconds = 1000,
        enableSingleFrameRendering = true,
    )
    val strategy = createStrategy(
        animationWidth = 100,
        animationHeight = 100,
        source = null,
        factory = factory,
    )
    val loaderField = FrameLoaderStrategy::class.java.getDeclaredField("frameLoader")
    loaderField.isAccessible = true
    strategy.prepareFrames(canvasWidth = 100, canvasHeight = 100, onAnimationLoaded = null)
    val beforeStop = loaderField.get(strategy)

    strategy.onStop()
    FrameLoaderFactory.clearUnusedUntil(java.util.Date(Long.MAX_VALUE))
    strategy.prepareFrames(canvasWidth = 100, canvasHeight = 100, onAnimationLoaded = null)

    org.junit.Assert.assertNotSame(beforeStop, loaderField.get(strategy))
  }

  private fun createStrategy(
      animationWidth: Int,
      animationHeight: Int,
      source: String? = "test",
      factory: FrameLoaderFactory = frameLoaderFactory,
  ): FrameLoaderStrategy {
    val animationInformation = mock<AnimationInformation>()
    whenever(animationInformation.width()).thenReturn(animationWidth)
    whenever(animationInformation.height()).thenReturn(animationHeight)
    whenever(animationInformation.frameCount).thenReturn(1)
    whenever(animationInformation.loopDurationMs).thenReturn(1000)
    return FrameLoaderStrategy(
        source = source,
        animationInformation = animationInformation,
        bitmapFrameRenderer = bitmapFrameRenderer,
        frameLoaderFactory = factory,
        downscaleFrameToDrawableDimensions = true,
    )
  }
}
