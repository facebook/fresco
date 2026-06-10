/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.graphics.Bitmap
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry
import com.facebook.common.references.CloseableReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that bitmap pools skip [Bitmap.recycle] when CloseableReferences for bitmaps are in
 * [CloseableReference.REF_TYPE_NOOP] mode AND the skip-recycle A/B test is enabled. Under NOOP refs
 * a bitmap may still be referenced/drawn when a pool frees it, so it must be left for the GC to
 * reclaim instead of being recycled.
 */
@RunWith(RobolectricTestRunner::class)
class BitmapPoolNoopRecycleTest {

  @After
  fun reset() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(false)
  }

  @Test
  fun dummyBitmapPool_release_whenNoopRefsAndSkipEnabled_doesNotRecycle() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_NOOP)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    val pool = DummyBitmapPool()
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    pool.release(bitmap)

    assertThat(bitmap.isRecycled).isFalse()
  }

  @Test
  fun dummyBitmapPool_release_whenNoopRefsButSkipDisabled_recyclesAsBefore() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_NOOP)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(false)
    val pool = DummyBitmapPool()
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    pool.release(bitmap)

    assertThat(bitmap.isRecycled).isTrue()
  }

  @Test
  fun dummyBitmapPool_release_whenDefaultRefs_recyclesAsBefore() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    val pool = DummyBitmapPool()
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    pool.release(bitmap)

    assertThat(bitmap.isRecycled).isTrue()
  }

  @Test
  fun dummyTrackingInUseBitmapPool_release_whenNoopRefsAndSkipEnabled_doesNotRecycle() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_NOOP)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    val pool = DummyTrackingInUseBitmapPool()
    val bitmap = pool.get(4)

    pool.release(bitmap)

    assertThat(bitmap.isRecycled).isFalse()
  }

  @Test
  fun dummyTrackingInUseBitmapPool_release_whenDefaultRefs_recyclesAsBefore() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    val pool = DummyTrackingInUseBitmapPool()
    val bitmap = pool.get(4)

    pool.release(bitmap)

    assertThat(bitmap.isRecycled).isTrue()
  }

  @Test
  fun bucketsBitmapPool_release_whenNoopRefsAndSkipEnabled_doesNotRecycle() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_NOOP)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    val pool = newBucketsBitmapPool()
    // With empty buckets (no pooling), release() frees the bitmap directly.
    val bitmap = pool.get(12)

    pool.release(bitmap)

    assertThat(bitmap.isRecycled).isFalse()
  }

  @Test
  fun bucketsBitmapPool_release_whenDefaultRefs_recyclesAsBefore() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    val pool = newBucketsBitmapPool()
    // With empty buckets (no pooling), release() frees the bitmap directly.
    val bitmap = pool.get(12)

    pool.release(bitmap)

    assertThat(bitmap.isRecycled).isTrue()
  }

  @Test
  fun shouldSkipBitmapRecycleForNoopRefs_requiresBothNoopRefsAndSkipEnabled() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(false)
    assertThat(CloseableReference.shouldSkipBitmapRecycleForNoopRefs()).isFalse()

    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    assertThat(CloseableReference.shouldSkipBitmapRecycleForNoopRefs()).isFalse()

    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_NOOP)
    CloseableReference.setSkipBitmapRecycleForNoopRefs(false)
    assertThat(CloseableReference.shouldSkipBitmapRecycleForNoopRefs()).isFalse()

    CloseableReference.setSkipBitmapRecycleForNoopRefs(true)
    assertThat(CloseableReference.shouldSkipBitmapRecycleForNoopRefs()).isTrue()
  }

  private fun newBucketsBitmapPool(): BucketsBitmapPool =
      BucketsBitmapPool(
          NoOpMemoryTrimmableRegistry.getInstance(),
          DefaultBitmapPoolParams.get(),
          NoOpPoolStatsTracker.getInstance(),
      )
}
