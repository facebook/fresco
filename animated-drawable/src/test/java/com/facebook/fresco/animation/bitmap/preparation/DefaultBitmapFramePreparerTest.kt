/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests [DefaultBitmapFramePreparer]. */
@RunWith(RobolectricTestRunner::class)
class DefaultBitmapFramePreparerTest {

  companion object {
    private const val FRAME_COUNT = 10
    private const val BACKEND_INTRINSIC_WIDTH = 160
    private const val BACKEND_INTRINSIC_HEIGHT = 90
    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
  }

  private lateinit var animationBackend: AnimationBackend
  private lateinit var bitmapFrameCache: BitmapFrameCache
  private lateinit var platformBitmapFactory: PlatformBitmapFactory
  private lateinit var bitmapFrameRenderer: BitmapFrameRenderer
  private lateinit var bitmapReference: CloseableReference<Bitmap>
  private lateinit var bitmap: Bitmap

  private lateinit var fakeClock: FakeClock
  private lateinit var executorService: TestExecutorService
  private lateinit var defaultBitmapFramePreparer: DefaultBitmapFramePreparer

  @Before
  fun setup() {
    animationBackend = mock()
    bitmapFrameCache = mock()
    platformBitmapFactory = mock()
    bitmapFrameRenderer = mock()
    bitmapReference = mock()
    bitmap = mock()

    fakeClock = FakeClock()
    executorService = TestExecutorService(fakeClock)

    defaultBitmapFramePreparer =
        DefaultBitmapFramePreparer(
            platformBitmapFactory, bitmapFrameRenderer, BITMAP_CONFIG, executorService)
    whenever(animationBackend.frameCount).thenReturn(FRAME_COUNT)
    whenever(animationBackend.intrinsicWidth).thenReturn(BACKEND_INTRINSIC_WIDTH)
    whenever(animationBackend.intrinsicHeight).thenReturn(BACKEND_INTRINSIC_HEIGHT)
    whenever(bitmapReference.isValid).thenReturn(true)
    whenever(bitmapReference.get()).thenReturn(bitmap)
  }

  @Test
  fun testPrepareFrame_whenBitmapAlreadyCached_thenDoNothing() {
    whenever(bitmapFrameCache.contains(1)).thenReturn(true)
    whenever(bitmapFrameRenderer.renderFrame(1, bitmap)).thenReturn(true)

    defaultBitmapFramePreparer.prepareFrame(bitmapFrameCache, animationBackend, 1)

    assertThat(executorService.scheduledQueue.isIdle).isTrue()

    verify(bitmapFrameCache).contains(1)
    verifyNoMoreInteractions(bitmapFrameCache)
    verifyNoMoreInteractions(platformBitmapFactory, bitmapFrameRenderer, bitmapReference)
  }

  @Test
  fun testPrepareFrame_whenNoBitmapAvailable_thenDoNothing() {
    defaultBitmapFramePreparer.prepareFrame(bitmapFrameCache, animationBackend, 1)

    verify(bitmapFrameCache).contains(1)
    verifyNoMoreInteractions(bitmapFrameCache)
    reset(bitmapFrameCache)

    executorService.scheduledQueue.runNextPendingCommand()

    verify(bitmapFrameCache).contains(1)
    verify(bitmapFrameCache)
        .getBitmapToReuseForFrame(1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT)
    verifyNoMoreInteractions(bitmapFrameCache)

    verify(platformBitmapFactory)
        .createBitmap(BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT, BITMAP_CONFIG)

    verifyNoMoreInteractions(bitmapFrameRenderer)
  }

  @Test
  fun testPrepareFrame_whenReusedBitmapAvailable_thenCacheReusedBitmap() {
    whenever(
            bitmapFrameCache.getBitmapToReuseForFrame(
                1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT))
        .thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(1, bitmap)).thenReturn(true)

    defaultBitmapFramePreparer.prepareFrame(bitmapFrameCache, animationBackend, 1)

    executorService.scheduledQueue.runNextPendingCommand()

    verify(bitmapFrameCache, times(2)).contains(1)
    verify(bitmapFrameCache)
        .getBitmapToReuseForFrame(1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT)
    verify(bitmapFrameRenderer).renderFrame(1, bitmap)

    verify(bitmapFrameCache)
        .onFramePrepared(1, bitmapReference, BitmapAnimationBackend.FRAME_TYPE_REUSED)

    verifyNoMoreInteractions(platformBitmapFactory)
  }

  @Test
  fun testPrepareFrame_whenPlatformBitmapAvailable_thenCacheCreatedBitmap() {
    whenever(
            platformBitmapFactory.createBitmap(
                BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT, BITMAP_CONFIG))
        .thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(1, bitmap)).thenReturn(true)

    defaultBitmapFramePreparer.prepareFrame(bitmapFrameCache, animationBackend, 1)

    executorService.scheduledQueue.runNextPendingCommand()

    verify(bitmapFrameCache, times(2)).contains(1)
    verify(bitmapFrameCache)
        .getBitmapToReuseForFrame(1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT)
    verify(platformBitmapFactory)
        .createBitmap(BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT, BITMAP_CONFIG)
    verify(bitmapFrameRenderer).renderFrame(1, bitmap)

    verify(bitmapFrameCache)
        .onFramePrepared(1, bitmapReference, BitmapAnimationBackend.FRAME_TYPE_CREATED)

    verifyNoMoreInteractions(platformBitmapFactory)
  }

  @Test
  fun testPrepareFrame_whenReusedAndPlatformBitmapAvailable_thenCacheReusedBitmap() {
    whenever(
            bitmapFrameCache.getBitmapToReuseForFrame(
                1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT))
        .thenReturn(bitmapReference)
    whenever(
            platformBitmapFactory.createBitmap(
                BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT, BITMAP_CONFIG))
        .thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(1, bitmap)).thenReturn(true)

    defaultBitmapFramePreparer.prepareFrame(bitmapFrameCache, animationBackend, 1)

    executorService.scheduledQueue.runNextPendingCommand()

    verify(bitmapFrameCache, times(2)).contains(1)
    verify(bitmapFrameCache)
        .getBitmapToReuseForFrame(1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT)
    verify(bitmapFrameRenderer).renderFrame(1, bitmap)

    verify(bitmapFrameCache)
        .onFramePrepared(1, bitmapReference, BitmapAnimationBackend.FRAME_TYPE_REUSED)

    verifyNoMoreInteractions(platformBitmapFactory)
  }

  @Test
  fun testPrepareFrame_whenRenderingFails_thenDoNothing() {
    whenever(
            bitmapFrameCache.getBitmapToReuseForFrame(
                1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT))
        .thenReturn(bitmapReference)
    whenever(
            platformBitmapFactory.createBitmap(
                BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT, BITMAP_CONFIG))
        .thenReturn(bitmapReference)
    whenever(bitmapFrameRenderer.renderFrame(1, bitmap)).thenReturn(false)

    defaultBitmapFramePreparer.prepareFrame(bitmapFrameCache, animationBackend, 1)

    executorService.scheduledQueue.runNextPendingCommand()

    verify(bitmapFrameCache, times(2)).contains(1)
    verify(bitmapFrameCache)
        .getBitmapToReuseForFrame(1, BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT)
    verify(platformBitmapFactory)
        .createBitmap(BACKEND_INTRINSIC_WIDTH, BACKEND_INTRINSIC_HEIGHT, BITMAP_CONFIG)
    verify(bitmapFrameRenderer, times(2)).renderFrame(1, bitmap)

    verifyNoMoreInteractions(bitmapFrameCache)
  }
}
