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
import com.facebook.fresco.vito.options.AnimatedOptions
import com.facebook.fresco.vito.options.RoundingOptions
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
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
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
    verifyNoMoreInteractions(canvas, bitmapFrameCache)
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

  fun createBackendWithRounding(roundingOptions: RoundingOptions?): BitmapAnimationBackend {
    return BitmapAnimationBackend(
        platformBitmapFactory,
        bitmapFrameCache,
        animationInformation,
        bitmapFrameRenderer,
        false,
        bitmapFramePreparationStrategy,
        bitmapFramePreparer,
        roundingOptions)
  }

  /** Verifies circular rounding options are preserved and cornerRadii is null */
  @Test
  fun testCircularRoundingInitialization() {
    val circularRounding = RoundingOptions.asCircle()
    val backend = createBackendWithRounding(circularRounding)

    assertThat(backend.roundingOptions).isEqualTo(circularRounding)
    assertThat(backend.cornerRadii).isNull()
  }

  /** Verifies rectangular rounding creates an 8-element cornerRadii array with correct values */
  @Test
  fun testRectangularRoundingInitialization() {
    val rectangularRounding = RoundingOptions.forCornerRadiusPx(20f)
    val backend = createBackendWithRounding(rectangularRounding)

    assertThat(backend.roundingOptions?.isCircular).isFalse()
    assertThat(backend.cornerRadii).isNotNull()
    assertThat(backend.cornerRadii).hasSize(8)
    backend.cornerRadii?.forEach { radius -> assertThat(radius).isEqualTo(20f) }
  }

  /** Verifies null rounding options result in null cornerRadii */
  @Test
  fun testNoRoundingInitialization() {
    val backend = createBackendWithRounding(null)

    assertThat(backend.roundingOptions).isNull()
    assertThat(backend.cornerRadii).isNull()
  }

  /** Verifies custom corner radii arrays are preserved correctly */
  @Test
  fun testRoundingWithCornerRadiiArray() {
    val cornerRadii = floatArrayOf(10f, 10f, 20f, 20f, 30f, 30f, 40f, 40f)
    val roundingOptions = RoundingOptions.forCornerRadii(cornerRadii)
    val backend = createBackendWithRounding(roundingOptions)

    assertThat(backend.roundingOptions?.isCircular).isFalse()
    assertThat(backend.cornerRadii).isEqualTo(cornerRadii)
  }

  /** Verifies unset corner radius results in null cornerRadii */
  @Test
  fun testRoundingWithUnsetCornerRadius() {
    val roundingOptions = RoundingOptions.forCornerRadiusPx(RoundingOptions.CORNER_RADIUS_UNSET)
    val backend = createBackendWithRounding(roundingOptions)

    assertThat(backend.roundingOptions?.isCircular).isFalse()
    assertThat(backend.cornerRadii).isNull()
  }

  /** Verifies circular rounding options can be accessed and isCircular flag is true */
  @Test
  fun testRoundingOptionsAccessibilityForCircular() {
    val circularRounding = RoundingOptions.asCircle()
    val backend = createBackendWithRounding(circularRounding)

    assertThat(backend.roundingOptions).isEqualTo(circularRounding)
    assertThat(backend.roundingOptions?.isCircular).isTrue()
  }

  /** Verifies rectangular rounding options can be accessed and isCircular flag is false */
  @Test
  fun testRoundingOptionsAccessibilityForRectangular() {
    val cornerRadius = 25f
    val rectangularRounding = RoundingOptions.forCornerRadiusPx(cornerRadius)
    val backend = createBackendWithRounding(rectangularRounding)

    assertThat(backend.roundingOptions?.cornerRadius).isEqualTo(cornerRadius)
    assertThat(backend.roundingOptions?.isCircular).isFalse()
  }

  /** Verifies frame drawing works correctly with circular rounding applied */
  @Test
  fun testDrawFrameWithCircularRounding() {
    val circularRounding = RoundingOptions.asCircle()
    val backend = createBackendWithRounding(circularRounding)
    backend.setFrameListener(frameListener)

    whenever(bitmapFrameCache.getCachedFrame(anyInt())).thenReturn(bitmapReference)

    val result = backend.drawFrame(parentDrawable, canvas, 1)

    assertThat(result).isTrue()
    verify(frameListener).onDrawFrameStart(backend, 1)
  }

  /** Verifies frame drawing works correctly with rectangular rounding applied */
  @Test
  fun testDrawFrameWithRectangularRounding() {
    val rectangularRounding = RoundingOptions.forCornerRadiusPx(20f)
    val backend = createBackendWithRounding(rectangularRounding)
    backend.setFrameListener(frameListener)

    whenever(bitmapFrameCache.getCachedFrame(anyInt())).thenReturn(bitmapReference)

    val result = backend.drawFrame(parentDrawable, canvas, 1)

    assertThat(result).isTrue()
    verify(frameListener).onDrawFrameStart(backend, 1)
  }

  /** Verifies bounds setting works correctly with circular rounding */
  @Test
  fun testCircularRoundingWithBounds() {
    val circularRounding = RoundingOptions.asCircle()
    val backend = createBackendWithRounding(circularRounding)

    val testBounds = Rect(0, 0, 150, 150)
    backend.setBounds(testBounds)

    verify(bitmapFrameRenderer).setBounds(testBounds)
  }

  /** Verifies empty corner radii arrays are handled correctly */
  @Test
  fun testRoundingOptionsWithEmptyCornerRadiiArray() {
    val emptyCornerRadii = floatArrayOf()
    val roundingOptions = RoundingOptions.forCornerRadii(emptyCornerRadii)
    val backend = createBackendWithRounding(roundingOptions)

    assertThat(backend.roundingOptions?.isCircular).isFalse()
    assertThat(backend.cornerRadii).isEqualTo(emptyCornerRadii)
  }

  /** Verifies cornerRadii is null specifically for circular rounding */
  @Test
  fun testCornerRadiiNullForCircularRounding() {
    val circularRounding = RoundingOptions.asCircle()
    val backend = createBackendWithRounding(circularRounding)

    // For circular rounding, cornerRadii should be null
    assertThat(backend.cornerRadii).isNull()
    assertThat(backend.roundingOptions?.isCircular).isTrue()
  }

  /** Verifies cornerRadii is a proper 8-element array for rectangular rounding */
  @Test
  fun testCornerRadiiArrayForRectangularRounding() {
    val cornerRadius = 15f
    val rectangularRounding = RoundingOptions.forCornerRadiusPx(cornerRadius)
    val backend = createBackendWithRounding(rectangularRounding)

    // For rectangular rounding, cornerRadii should be an array of 8 elements
    assertThat(backend.cornerRadii).isNotNull()
    assertThat(backend.cornerRadii).hasSize(8)
    backend.cornerRadii?.forEach { radius -> assertThat(radius).isEqualTo(cornerRadius) }
  }

  /** Verifies rounding options are preserved during backend creation */
  @Test
  fun testRoundingOptionsPreservation() {
    val originalRounding = RoundingOptions.asCircle()
    val backend = createBackendWithRounding(originalRounding)

    assertThat(backend.roundingOptions).isEqualTo(originalRounding)
  }

  /** Verifies custom corner radii arrays are preserved in both backend and options */
  @Test
  fun testRoundingWithCustomCornerRadiiPreservation() {
    val customRadii = floatArrayOf(5f, 5f, 10f, 10f, 15f, 15f, 20f, 20f)
    val roundingOptions = RoundingOptions.forCornerRadii(customRadii)
    val backend = createBackendWithRounding(roundingOptions)

    assertThat(backend.cornerRadii).isEqualTo(customRadii)
    assertThat(backend.roundingOptions?.cornerRadii).isEqualTo(customRadii)
  }

  /** Verifies circular rounding behavior when bounds are set and frames are drawn */
  @Test
  fun testCircularRoundingBehaviorWithBounds() {
    val circularRounding = RoundingOptions.asCircle()
    val backend = createBackendWithRounding(circularRounding)
    backend.setFrameListener(frameListener)

    val testBounds = Rect(0, 0, 100, 100)
    backend.setBounds(testBounds)

    whenever(bitmapFrameCache.getCachedFrame(anyInt())).thenReturn(bitmapReference)

    val result = backend.drawFrame(parentDrawable, canvas, 1)

    assertThat(result).isTrue()
    verify(bitmapFrameRenderer).setBounds(testBounds)
    verify(frameListener).onDrawFrameStart(backend, 1)
  }

  /** Verifies rectangular rounding behavior when bounds are set and frames are drawn */
  @Test
  fun testRectangularRoundingBehaviorWithBounds() {
    val rectangularRounding = RoundingOptions.forCornerRadiusPx(10f)
    val backend = createBackendWithRounding(rectangularRounding)
    backend.setFrameListener(frameListener)

    val testBounds = Rect(0, 0, 200, 150)
    backend.setBounds(testBounds)

    whenever(bitmapFrameCache.getCachedFrame(anyInt())).thenReturn(bitmapReference)

    val result = backend.drawFrame(parentDrawable, canvas, 1)

    assertThat(result).isTrue()
    verify(bitmapFrameRenderer).setBounds(testBounds)
    verify(frameListener).onDrawFrameStart(backend, 1)
  }

  private fun createThumbnailBackend(
      thumbnailUrl: String?,
      loopCount: Int,
      roundingOptions: RoundingOptions? = null
  ): BitmapAnimationBackend {
    val animatedOptions =
        if (thumbnailUrl != null) {
          AnimatedOptions.loop(loopCount, thumbnailUrl)
        } else {
          AnimatedOptions.loop(loopCount)
        }
    return BitmapAnimationBackend(
        platformBitmapFactory,
        bitmapFrameCache,
        animationInformation,
        bitmapFrameRenderer,
        false,
        bitmapFramePreparationStrategy,
        bitmapFramePreparer,
        roundingOptions,
        animatedOptions)
  }

  private fun createBackendWithAnimatedOptions(
      animatedOptions: AnimatedOptions?
  ): BitmapAnimationBackend {
    return BitmapAnimationBackend(
        platformBitmapFactory,
        bitmapFrameCache,
        animationInformation,
        bitmapFrameRenderer,
        false,
        bitmapFramePreparationStrategy,
        bitmapFramePreparer,
        null,
        animatedOptions)
  }

  private fun setupAnimationInformation(frameCount: Int = 3, loopCount: Int = 1) {
    whenever(animationInformation.frameCount).thenReturn(frameCount)
    whenever(animationInformation.loopCount).thenReturn(loopCount)
  }

  /**
   * Tests that thumbnail fallback is enabled when valid thumbnail URL and finite loop count are
   * provided.
   */
  @Test
  fun testThumbnailInitializationWithValidOptions() {
    val thumbnailUrl = "https://example.com/thumbnail.jpg"
    val backend = createThumbnailBackend(thumbnailUrl, 3)

    assertThat(backend.animatedOptions?.useFallbackThumbnail()).isTrue()
    assertThat(backend.animatedOptions?.thumbnailUrl).isEqualTo(thumbnailUrl)
  }

  /** Tests that backend handles null AnimatedOptions without errors. */
  @Test
  fun testThumbnailInitializationWithNullOptions() {
    val backend = createBackendWithAnimatedOptions(null)
    assertThat(backend.animatedOptions).isNull()
  }

  /** Tests that thumbnail fallback is disabled when empty thumbnail URL is provided. */
  @Test
  fun testThumbnailInitializationWithEmptyUrl() {
    val backend = createThumbnailBackend("", 3)
    assertThat(backend.animatedOptions?.useFallbackThumbnail()).isFalse()
  }

  /** Tests that infinite loop animations don't use thumbnail fallback even with valid URL. */
  @Test
  fun testThumbnailInitializationWithInfiniteLoop() {
    val backend =
        createThumbnailBackend("https://example.com/thumb.jpg", AnimatedOptions.LOOP_COUNT_INFINITE)
    assertThat(backend.animatedOptions?.useFallbackThumbnail()).isFalse()
  }

  /** Tests that AnimatedOptions loop count overrides AnimationInformation loop count. */
  @Test
  fun testLoopCountWithAnimatedOptions() {
    setupAnimationInformation(loopCount = 10)
    val backend = createBackendWithAnimatedOptions(AnimatedOptions.loop(5))

    assertThat(backend.loopCount).isEqualTo(5)
  }

  /** Tests that infinite AnimatedOptions correctly returns infinite loop count. */
  @Test
  fun testLoopCountWithInfiniteAnimatedOptions() {
    setupAnimationInformation(loopCount = 10)
    val backend = createBackendWithAnimatedOptions(AnimatedOptions.infinite())

    assertThat(backend.loopCount).isEqualTo(AnimationInformation.LOOP_COUNT_INFINITE)
  }

  /**
   * Tests that backend falls back to AnimationInformation loop count when no AnimatedOptions
   * provided.
   */
  @Test
  fun testLoopCountWithoutAnimatedOptions() {
    setupAnimationInformation(loopCount = 7)
    val backend = createBackendWithAnimatedOptions(null)

    assertThat(backend.loopCount).isEqualTo(7)
  }

  /** Tests that normal frame drawing works when animation hasn't completed yet. */
  @Test
  fun testDrawFrameWithThumbnailFallback() {
    setupAnimationInformation(frameCount = 3)
    whenever(bitmapFrameCache.getCachedFrame(anyInt())).thenReturn(bitmapReference)

    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 2)
    backend.setFrameListener(frameListener)

    val result = backend.drawFrame(parentDrawable, canvas, 0)

    assertThat(result).isTrue()
    verify(frameListener).onDrawFrameStart(backend, 0)
    verify(canvas).drawBitmap(eq(bitmap), eq(0f), eq(0f), any<Paint>())
  }

  /**
   * Tests the useFallbackThumbnail() logic with various URL and loop configurations including
   * static options.
   */
  @Test
  fun testAnimatedOptionsUseFallbackThumbnail() {
    val validOptions = AnimatedOptions.loop(3, "https://example.com/thumb.jpg")
    assertThat(validOptions.useFallbackThumbnail()).isTrue()

    val emptyUrlOptions = AnimatedOptions.loop(3, "")
    assertThat(emptyUrlOptions.useFallbackThumbnail()).isFalse()

    val nullUrlOptions = AnimatedOptions.loop(3, null)
    assertThat(nullUrlOptions.useFallbackThumbnail()).isFalse()

    val infiniteOptions = AnimatedOptions.infinite()
    assertThat(infiniteOptions.useFallbackThumbnail()).isFalse()

    val staticOptions = AnimatedOptions.static()
    assertThat(staticOptions.useFallbackThumbnail()).isFalse()
  }

  /** Tests equality and hashCode methods for AnimatedOptions with thumbnail URLs. */
  @Test
  fun testAnimatedOptionsEquality() {
    val options1 = AnimatedOptions.loop(3, "https://example.com/thumb.jpg")
    val options2 = AnimatedOptions.loop(3, "https://example.com/thumb.jpg")
    val options3 = AnimatedOptions.loop(3, "https://different.com/thumb.jpg")
    val options4 = AnimatedOptions.loop(5, "https://example.com/thumb.jpg")

    assertThat(options1).isEqualTo(options2)
    assertThat(options1).isNotEqualTo(options3)
    assertThat(options1).isNotEqualTo(options4)

    assertThat(options1.hashCode()).isEqualTo(options2.hashCode())
  }

  /** Tests that thumbnail and rounding options work together correctly. */
  @Test
  fun testAnimatedOptionsWithRoundingOptions() {
    val roundingOptions = RoundingOptions.asCircle()
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 3, roundingOptions)

    assertThat(backend.animatedOptions?.useFallbackThumbnail()).isTrue()
    assertThat(backend.roundingOptions).isEqualTo(roundingOptions)
  }

  /** Tests that thumbnail resources are properly cleaned up when backend becomes inactive. */
  @Test
  fun testOnInactiveReleasesResources() {
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 3)
    backend.onInactive()

    verify(bitmapFrameCache).clear()
  }

  /** Tests that setting bounds works correctly when thumbnail drawable is present. */
  @Test
  fun testSetBoundsWithThumbnailDrawable() {
    val testBounds = Rect(10, 20, 110, 120)
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 3)

    backend.setBounds(testBounds)

    verify(bitmapFrameRenderer).setBounds(testBounds)
  }

  /** Tests that setting null bounds with thumbnail drawable doesn't cause errors. */
  @Test
  fun testSetBoundsWithNullBoundsAndThumbnail() {
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 3)

    backend.setBounds(null)

    verify(bitmapFrameRenderer).setBounds(null)
  }

  /** Tests that roundingOptions is accessible as a property for circular rounding. */
  @Test
  fun testRoundingOptionsAccessibility() {
    val roundingOptions = RoundingOptions.asCircle()
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 3, roundingOptions)

    assertThat(backend.roundingOptions).isEqualTo(roundingOptions)
    assertThat(backend.roundingOptions?.isCircular).isTrue()
  }

  /** Tests that corner radius rounding options are properly accessible and configured. */
  @Test
  fun testRoundingOptionsWithCornerRadius() {
    val cornerRadius = 15f
    val roundingOptions = RoundingOptions.forCornerRadiusPx(cornerRadius)
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 3, roundingOptions)

    assertThat(backend.roundingOptions?.cornerRadius).isEqualTo(cornerRadius)
    assertThat(backend.roundingOptions?.isCircular).isFalse()
  }

  /** Tests that null rounding options are handled correctly without errors. */
  @Test
  fun testRoundingOptionsWithNullRounding() {
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 3, null)
    assertThat(backend.roundingOptions).isNull()
  }

  /** Tests that circular rounding and thumbnail fallback work together seamlessly. */
  @Test
  fun testThumbnailWithCircularRounding() {
    val roundingOptions = RoundingOptions.asCircle()
    val backend = createThumbnailBackend("https://example.com/thumb.jpg", 2, roundingOptions)

    assertThat(backend.roundingOptions?.isCircular).isTrue()
    assertThat(backend.animatedOptions?.useFallbackThumbnail()).isTrue()
  }

  /** Tests that thumbnail fallback is disabled when null thumbnail URL is provided. */
  @Test
  fun testThumbnailFallbackDisabledWithoutUrl() {
    val backend = createThumbnailBackend(null, 3)
    assertThat(backend.animatedOptions?.useFallbackThumbnail()).isFalse()
  }

  /** Tests that empty string thumbnail URL disables thumbnail fallback functionality. */
  @Test
  fun testThumbnailFallbackDisabledWithEmptyUrl() {
    val backend = createThumbnailBackend("", 3)
    assertThat(backend.animatedOptions?.useFallbackThumbnail()).isFalse()
  }
}
