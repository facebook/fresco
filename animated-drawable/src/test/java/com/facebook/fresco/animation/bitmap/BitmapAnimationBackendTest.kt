/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparationStrategy
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests [BitmapAnimationBackend] */
@RunWith(RobolectricTestRunner::class)
class BitmapAnimationBackendTest {

  @Mock lateinit var platformBitmapFactory: PlatformBitmapFactory
  @Mock lateinit var bitmapFrameCache: BitmapFrameCache
  @Mock lateinit var animationInformation: AnimationInformation
  @Mock lateinit var bitmapFrameRenderer: BitmapFrameRenderer
  @Mock lateinit var bounds: Rect
  @Mock lateinit var parentDrawable: Drawable
  @Mock lateinit var canvas: Canvas
  @Mock lateinit var bitmap: Bitmap
  @Mock lateinit var bitmapResourceReleaser: ResourceReleaser<Bitmap>
  @Mock lateinit var frameListener: BitmapAnimationBackend.FrameListener
  @Mock lateinit var bitmapFramePreparationStrategy: BitmapFramePreparationStrategy
  @Mock lateinit var bitmapFramePreparer: BitmapFramePreparer
  @Captor lateinit var capturedBitmapReference: ArgumentCaptor<CloseableReference<Bitmap>>
  private lateinit var bitmapReference: CloseableReference<Bitmap>
  private lateinit var bitmapAnimationBackend: BitmapAnimationBackend

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    bitmapReference = CloseableReference.of(bitmap, bitmapResourceReleaser)
    bitmapAnimationBackend =
        BitmapAnimationBackend(
            platformBitmapFactory,
            bitmapFrameCache,
            animationInformation,
            bitmapFrameRenderer,
            false, /* isNewRenderImplementation */
            bitmapFramePreparationStrategy,
            bitmapFramePreparer,
            null)
    bitmapAnimationBackend.setFrameListener(frameListener)
  }

  @Test
  fun testSetBounds() {
    bitmapAnimationBackend.setBounds(bounds)
    verify(bitmapFrameRenderer).setBounds(bounds)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendDimensionsUnset() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    val backendIntrinsicHeight = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(bounds)

    assertThat(bitmapAnimationBackend.intrinsicWidth).isEqualTo(boundsWidth)
    assertThat(bitmapAnimationBackend.intrinsicHeight).isEqualTo(boundsHeight)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendDimensionWidthSet() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = 260
    val backendIntrinsicHeight = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(bounds)

    assertThat(bitmapAnimationBackend.intrinsicWidth).isEqualTo(backendIntrinsicWidth)
    assertThat(bitmapAnimationBackend.intrinsicHeight).isEqualTo(boundsHeight)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendDimensionHeightSet() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    val backendIntrinsicHeight = 260
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(bounds)

    assertThat(bitmapAnimationBackend.intrinsicWidth).isEqualTo(boundsWidth)
    assertThat(bitmapAnimationBackend.intrinsicHeight).isEqualTo(backendIntrinsicHeight)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendDimensionsSet() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = 260
    val backendIntrinsicHeight = 300
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(bounds)

    assertThat(bitmapAnimationBackend.intrinsicWidth).isEqualTo(backendIntrinsicWidth)
    assertThat(bitmapAnimationBackend.intrinsicHeight).isEqualTo(backendIntrinsicHeight)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendDimensionsUnsetAndNullBounds() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    val backendIntrinsicHeight = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(null)

    assertThat(bitmapAnimationBackend.intrinsicWidth)
        .isEqualTo(AnimationBackend.INTRINSIC_DIMENSION_UNSET)
    assertThat(bitmapAnimationBackend.intrinsicHeight)
        .isEqualTo(AnimationBackend.INTRINSIC_DIMENSION_UNSET)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendDimensionsSetAndNullBounds() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = 260
    val backendIntrinsicHeight = 300
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(null)

    assertThat(bitmapAnimationBackend.intrinsicWidth).isEqualTo(backendIntrinsicWidth)
    assertThat(bitmapAnimationBackend.intrinsicHeight).isEqualTo(backendIntrinsicHeight)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendWidthSetAndNullBounds() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = 260
    val backendIntrinsicHeight = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(null)

    assertThat(bitmapAnimationBackend.intrinsicWidth).isEqualTo(backendIntrinsicWidth)
    assertThat(bitmapAnimationBackend.intrinsicHeight).isEqualTo(backendIntrinsicHeight)
  }

  @Test
  fun testSetBoundsUpdatesIntrinsicDimensionsWhenBackendHeightSetAndNullBounds() {
    val boundsWidth = 160
    val boundsHeight = 90
    val backendIntrinsicWidth = AnimationBackend.INTRINSIC_DIMENSION_UNSET
    val backendIntrinsicHeight = 400
    setupBoundsAndRendererDimensions(
        boundsWidth, boundsHeight, backendIntrinsicWidth, backendIntrinsicHeight)

    bitmapAnimationBackend.setBounds(null)

    assertThat(bitmapAnimationBackend.intrinsicWidth).isEqualTo(backendIntrinsicWidth)
    assertThat(bitmapAnimationBackend.intrinsicHeight).isEqualTo(backendIntrinsicHeight)
  }

  @Test
  fun testGetFrameCount() {
    whenever(animationInformation.frameCount).thenReturn(123)
    assertThat(bitmapAnimationBackend.frameCount).isEqualTo(123)
  }

  @Test
  fun testGetLoopCount() {
    whenever(animationInformation.loopCount).thenReturn(AnimationInformation.LOOP_COUNT_INFINITE)
    assertThat(bitmapAnimationBackend.loopCount).isEqualTo(AnimationInformation.LOOP_COUNT_INFINITE)

    whenever(animationInformation.loopCount).thenReturn(123)
    assertThat(bitmapAnimationBackend.loopCount).isEqualTo(123)
  }

  @Test
  fun testGetFrameDuration() {
    whenever(animationInformation.getFrameDurationMs(1)).thenReturn(50)
    whenever(animationInformation.getFrameDurationMs(2)).thenReturn(100)

    assertThat(bitmapAnimationBackend.getFrameDurationMs(1)).isEqualTo(50)
    assertThat(bitmapAnimationBackend.getFrameDurationMs(2)).isEqualTo(100)
  }

  @Test
  fun testDrawCachedBitmap() {
    whenever(bitmapFrameCache.getCachedFrame(anyInt())).thenReturn(bitmapReference)

    bitmapAnimationBackend.drawFrame(parentDrawable, canvas, 1)
    verify(frameListener).onDrawFrameStart(bitmapAnimationBackend, 1)
    verify(bitmapFrameCache).getCachedFrame(1)
    verify(canvas).drawBitmap(eq(bitmap), eq(0f), eq(0f), any<Paint>())
    verifyFramePreparationStrategyCalled(1)
    assertReferencesClosed()
  }

  @Test
  fun testDrawReusedBitmap() {
    whenever(bitmapFrameCache.getBitmapToReuseForFrame(anyInt(), anyInt(), anyInt()))
        .thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(anyInt(), any<Bitmap>())).thenReturn(true)

    bitmapAnimationBackend.drawFrame(parentDrawable, canvas, 1)

    verify(frameListener).onDrawFrameStart(bitmapAnimationBackend, 1)
    verify(bitmapFrameCache).getCachedFrame(1)
    verify(bitmapFrameCache).getBitmapToReuseForFrame(1, 0, 0)
    verify(bitmapFrameRenderer).renderFrame(1, bitmap)
    verify(canvas).drawBitmap(eq(bitmap), eq(0f), eq(0f), any<Paint>())
    verifyFramePreparationStrategyCalled(1)
    verifyListenersAndCacheNotified(1, BitmapAnimationBackend.FRAME_TYPE_REUSED)
    assertReferencesClosed()
  }

  @Test
  fun testDrawNewBitmap() {
    whenever(platformBitmapFactory.createBitmap(anyInt(), anyInt(), any<Bitmap.Config>()))
        .thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(anyInt(), any<Bitmap>())).thenReturn(true)

    bitmapAnimationBackend.drawFrame(parentDrawable, canvas, 2)

    verify(frameListener).onDrawFrameStart(bitmapAnimationBackend, 2)
    verify(bitmapFrameCache).getCachedFrame(2)
    verify(bitmapFrameCache).getBitmapToReuseForFrame(2, 0, 0)
    verify(platformBitmapFactory).createBitmap(0, 0, Bitmap.Config.ARGB_8888)
    verify(bitmapFrameRenderer).renderFrame(2, bitmap)
    verify(canvas).drawBitmap(eq(bitmap), eq(0f), eq(0f), any<Paint>())
    verifyFramePreparationStrategyCalled(2)
    verifyListenersAndCacheNotified(2, BitmapAnimationBackend.FRAME_TYPE_CREATED)
    assertReferencesClosed()
  }

  @Test
  fun testDrawNewBitmapWithBounds() {
    val width = 160
    val height = 90
    whenever(bounds.width()).thenReturn(width)
    whenever(bounds.height()).thenReturn(height)

    whenever(platformBitmapFactory.createBitmap(anyInt(), anyInt(), any<Bitmap.Config>()))
        .thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(anyInt(), any<Bitmap>())).thenReturn(true)
    whenever(bitmapFrameRenderer.intrinsicWidth)
        .thenReturn(AnimationBackend.INTRINSIC_DIMENSION_UNSET)
    whenever(bitmapFrameRenderer.intrinsicHeight)
        .thenReturn(AnimationBackend.INTRINSIC_DIMENSION_UNSET)

    bitmapAnimationBackend.setBounds(bounds)
    bitmapAnimationBackend.drawFrame(parentDrawable, canvas, 2)

    verify(frameListener).onDrawFrameStart(bitmapAnimationBackend, 2)
    verify(bitmapFrameCache).getCachedFrame(2)
    verify(bitmapFrameCache).getBitmapToReuseForFrame(2, width, height)
    verify(platformBitmapFactory).createBitmap(width, height, Bitmap.Config.ARGB_8888)
    verify(bitmapFrameRenderer).renderFrame(2, bitmap)
    verify(canvas)
        .drawBitmap(eq(bitmap), ArgumentMatchers.isNull(Rect::class.java), eq(bounds), any<Paint>())
    verifyFramePreparationStrategyCalled(2)
    verifyListenersAndCacheNotified(2, BitmapAnimationBackend.FRAME_TYPE_CREATED)
    assertReferencesClosed()
  }

  @Test
  fun testDrawFallbackBitmapWhenCreateBitmapNotWorking() {
    whenever(bitmapFrameCache.getFallbackFrame(anyInt())).thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(anyInt(), any<Bitmap>())).thenReturn(true)

    bitmapAnimationBackend.drawFrame(parentDrawable, canvas, 3)

    verify(frameListener).onDrawFrameStart(bitmapAnimationBackend, 3)
    verify(bitmapFrameCache).getCachedFrame(3)
    verify(bitmapFrameCache).getBitmapToReuseForFrame(3, 0, 0)
    verify(platformBitmapFactory).createBitmap(0, 0, Bitmap.Config.ARGB_8888)
    verify(bitmapFrameCache).getFallbackFrame(3)
    verify(canvas).drawBitmap(eq(bitmap), eq(0f), eq(0f), any<Paint>())
    verifyFramePreparationStrategyCalled(3)
    verifyListenersNotifiedWithoutCache(3, BitmapAnimationBackend.FRAME_TYPE_FALLBACK)
    assertReferencesClosed()
  }

  @Test
  fun testDrawFallbackBitmapWhenRenderFrameNotWorking() {
    whenever(bitmapFrameCache.getFallbackFrame(anyInt())).thenReturn(bitmapReference)

    // Return a different bitmap for PlatformBitmapFactory
    val temporaryBitmap = CloseableReference.of(bitmap, bitmapResourceReleaser)
    whenever(platformBitmapFactory.createBitmap(anyInt(), anyInt(), any<Bitmap.Config>()))
        .thenReturn(temporaryBitmap)
    whenever(bitmapFrameRenderer.renderFrame(anyInt(), any<Bitmap>())).thenReturn(false)

    bitmapAnimationBackend.drawFrame(parentDrawable, canvas, 3)

    verify(frameListener).onDrawFrameStart(bitmapAnimationBackend, 3)
    verify(bitmapFrameCache).getCachedFrame(3)
    verify(bitmapFrameCache).getBitmapToReuseForFrame(3, 0, 0)
    verify(platformBitmapFactory).createBitmap(0, 0, Bitmap.Config.ARGB_8888)
    // Verify that the bitmap has been closed
    assertThat(temporaryBitmap.isValid).isFalse()
    verify(bitmapFrameCache).getFallbackFrame(3)
    verify(canvas).drawBitmap(eq(bitmap), eq(0f), eq(0f), any<Paint>())
    verifyFramePreparationStrategyCalled(3)
    verifyListenersNotifiedWithoutCache(3, BitmapAnimationBackend.FRAME_TYPE_FALLBACK)
    assertReferencesClosed()
  }

  @Test
  fun testDrawNoFrame() {
    bitmapAnimationBackend.drawFrame(parentDrawable, canvas, 4)

    verify(frameListener).onDrawFrameStart(bitmapAnimationBackend, 4)
    verify(bitmapFrameCache).getCachedFrame(4)
    verify(bitmapFrameCache).getBitmapToReuseForFrame(4, 0, 0)
    verify(platformBitmapFactory).createBitmap(0, 0, Bitmap.Config.ARGB_8888)
    verify(bitmapFrameCache).getFallbackFrame(4)
    Mockito.verifyNoMoreInteractions(canvas, bitmapFrameCache)
    verifyFramePreparationStrategyCalled(4)
    verify(frameListener).onFrameDropped(bitmapAnimationBackend, 4)
  }

  private fun verifyFramePreparationStrategyCalled(frameNumber: Int) {
    verify(bitmapFramePreparationStrategy)
        .prepareFrames(
            bitmapFramePreparer, bitmapFrameCache, bitmapAnimationBackend, frameNumber, null)
  }

  private fun verifyListenersAndCacheNotified(
      frameNumber: Int,
      @BitmapAnimationBackend.FrameType frameType: Int
  ) {
    // Verify cache callback
    verify(bitmapFrameCache)
        .onFrameRendered(eq(frameNumber), capture(capturedBitmapReference), eq(frameType))
    assertThat(capturedBitmapReference.value).isEqualTo(bitmapReference)

    // Verify frame listener
    verify(frameListener).onFrameDrawn(bitmapAnimationBackend, frameNumber, frameType)
  }

  private fun verifyListenersNotifiedWithoutCache(
      frameNumber: Int,
      @BitmapAnimationBackend.FrameType frameType: Int
  ) {
    // Verify cache callback
    verify(bitmapFrameCache, never()).onFrameRendered(anyInt(), any(), eq(frameType))

    // Verify frame listener
    verify(frameListener).onFrameDrawn(bitmapAnimationBackend, frameNumber, frameType)
  }

  private fun assertReferencesClosed() {
    assertThat(bitmapReference.isValid).isFalse()
  }

  private fun setupBoundsAndRendererDimensions(
      boundsWidth: Int,
      boundsHeight: Int,
      backendIntrinsicWidth: Int,
      backendIntrinsicHeight: Int
  ) {
    whenever(bounds.width()).thenReturn(boundsWidth)
    whenever(bounds.height()).thenReturn(boundsHeight)

    whenever(bitmapFrameRenderer.intrinsicWidth).thenReturn(backendIntrinsicWidth)
    whenever(bitmapFrameRenderer.intrinsicHeight).thenReturn(backendIntrinsicHeight)
  }
}
