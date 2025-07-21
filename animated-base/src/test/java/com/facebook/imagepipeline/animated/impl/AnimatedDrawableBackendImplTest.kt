/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.impl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.facebook.imagepipeline.animated.base.AnimatedImage
import com.facebook.imagepipeline.animated.base.AnimatedImageFrame
import com.facebook.imagepipeline.animated.base.AnimatedImageResult
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimatedDrawableBackendImplTest {
  private lateinit var animatedDrawableUtil: AnimatedDrawableUtil
  private lateinit var animatedImageResult: AnimatedImageResult
  private lateinit var canvas: Canvas
  private lateinit var image: AnimatedImage
  private lateinit var frame: AnimatedImageFrame
  private lateinit var bitmap: Bitmap
  private lateinit var rect: Rect
  private lateinit var mockedBitmap: MockedStatic<Bitmap>

  @Before
  @Throws(Exception::class)
  fun setUp() {
    animatedDrawableUtil = mock()
    animatedImageResult = mock()
    canvas = mock()
    image = mock()
    frame = mock()
    bitmap = mock()
    rect = mock()

    mockedBitmap = Mockito.mockStatic(Bitmap::class.java)

    whenever(animatedImageResult.image).thenReturn(image)
    whenever(image.doesRenderSupportScaling()).thenReturn(false)
    whenever(image.getFrame(any<Int>())).thenReturn(frame)

    mockedBitmap
        .`when`<Bitmap> {
          Bitmap.createBitmap(
              Mockito.anyInt(), Mockito.anyInt(), Mockito.any(Bitmap.Config::class.java))
        }
        .thenReturn(bitmap)
  }

  private fun verifyBasic(
      canvasWidth: Int,
      canvasHeight: Int,
      frameOriginalWidth: Int,
      frameOriginalHeight: Int,
      frameExpectedRenderedWidth: Int,
      frameExpectedRenderedHeight: Int
  ) {
    whenever(canvas.width).thenReturn(canvasWidth)
    whenever(canvas.height).thenReturn(canvasHeight)
    whenever(frame.width).thenReturn(frameOriginalWidth)
    whenever(frame.height).thenReturn(frameOriginalHeight)

    val animatedDrawableBackendImpl =
        AnimatedDrawableBackendImpl(animatedDrawableUtil, animatedImageResult, rect, true)

    animatedDrawableBackendImpl.renderFrame(0, canvas)

    verify(frame).renderFrame(frameExpectedRenderedWidth, frameExpectedRenderedHeight, bitmap)
  }

  @After
  fun tearDownStaticMocks() {
    mockedBitmap.close()
  }

  @Test
  fun testSimple() {
    verifyBasic(128, 128, 512, 512, 128, 128)
  }

  @Test
  fun testNoUpscaling() {
    verifyBasic(128, 128, 16, 16, 16, 16)
  }

  @Test
  fun testNarrow() {
    verifyBasic(64, 128, 256, 256, 64, 64)
  }

  @Test
  fun testOffsets() {
    val frameSide = 1024
    val canvasSide = 256
    val scale = frameSide / canvasSide

    val frameOffset = 512
    whenever(frame.xOffset).thenReturn(frameOffset)
    whenever(frame.yOffset).thenReturn(frameOffset)

    verifyBasic(canvasSide, canvasSide, frameSide, frameSide, frameSide / scale, frameSide / scale)
    verify(canvas).translate((frameOffset / scale).toFloat(), (frameOffset / scale).toFloat())
  }
}
