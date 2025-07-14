/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.wrapper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests [AnimatedDrawableBackendFrameRenderer] */
@RunWith(RobolectricTestRunner::class)
class AnimatedDrawableBackendFrameRendererTest {
  private lateinit var animatedDrawableBackendFrameRenderer: AnimatedDrawableBackendFrameRenderer
  private lateinit var animatedDrawableBackend: AnimatedDrawableBackend
  private lateinit var bitmapFrameCache: BitmapFrameCache

  @Before
  fun setup() {
    animatedDrawableBackend = mock()
    bitmapFrameCache = mock()
    animatedDrawableBackendFrameRenderer =
        AnimatedDrawableBackendFrameRenderer(bitmapFrameCache, animatedDrawableBackend, false)
  }

  @Test
  fun testSetBounds() {
    whenever(animatedDrawableBackend.forNewBounds(any<Rect>())).thenReturn(animatedDrawableBackend)

    val bounds: Rect = mock()
    animatedDrawableBackendFrameRenderer.setBounds(bounds)

    verify(animatedDrawableBackend).forNewBounds(bounds)
  }

  @Test
  fun testGetIntrinsicWidth() {
    whenever(animatedDrawableBackend.getWidth()).thenReturn(123)

    Assertions.assertThat(animatedDrawableBackendFrameRenderer.intrinsicWidth).isEqualTo(123)
    Assertions.assertThat(animatedDrawableBackendFrameRenderer.intrinsicHeight).isNotEqualTo(123)
  }

  @Test
  fun testGetIntrinsicHeight() {
    whenever(animatedDrawableBackend.getHeight()).thenReturn(1200)

    Assertions.assertThat(animatedDrawableBackendFrameRenderer.intrinsicHeight).isEqualTo(1200)
    Assertions.assertThat(animatedDrawableBackendFrameRenderer.intrinsicWidth).isNotEqualTo(1200)
  }

  @Test
  fun testRenderFrame() {
    whenever(animatedDrawableBackend.getHeight()).thenReturn(1200)
    val bitmap: Bitmap = mockBitmap()
    val animatedDrawableFrameInfo: AnimatedDrawableFrameInfo = mock()
    whenever(animatedDrawableBackend.getFrameInfo(any<Int>())).thenReturn(animatedDrawableFrameInfo)

    val rendered = animatedDrawableBackendFrameRenderer.renderFrame(0, bitmap)

    Assertions.assertThat(rendered).isTrue()
  }

  @Test
  fun testRenderFrameUnsuccessful() {
    val frameNumber = 0

    whenever(animatedDrawableBackend.getHeight()).thenReturn(1200)
    val bitmap: Bitmap = mockBitmap()
    val animatedDrawableFrameInfo: AnimatedDrawableFrameInfo = mock()
    whenever(animatedDrawableBackend.getFrameInfo(any<Int>())).thenReturn(animatedDrawableFrameInfo)
    doThrow(IllegalStateException())
        .whenever(animatedDrawableBackend)
        .renderFrame(eq(frameNumber), any<Canvas>())

    val rendered = animatedDrawableBackendFrameRenderer.renderFrame(frameNumber, bitmap)

    Assertions.assertThat(rendered).isFalse()
  }

  companion object {
    private fun mockBitmap(): Bitmap {
      val mock: Bitmap = mock()
      whenever(mock.isMutable).thenReturn(true)
      return mock
    }
  }
}
