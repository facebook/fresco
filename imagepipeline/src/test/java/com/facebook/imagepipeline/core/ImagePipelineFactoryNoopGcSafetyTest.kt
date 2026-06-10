/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import com.facebook.cache.common.CacheKey
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory
import com.facebook.imagepipeline.cache.CountingLruBitmapMemoryCacheFactory
import com.facebook.imagepipeline.cache.CountingMemoryCache
import com.facebook.imagepipeline.cache.MemoryCache
import com.facebook.imagepipeline.cache.MemoryCacheParams
import com.facebook.imagepipeline.image.CloseableImage
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies [ImagePipelineFactory.isNoopWithNonGcSafeFactory], the predicate that backs the
 * [com.facebook.common.logging.FLog] warning emitted when bitmap [CloseableReference]s are in
 * [CloseableReference.REF_TYPE_NOOP] mode but the configured bitmap cache factory is not GC-safe.
 * Under NOOP refs a counting cache's client count never reaches zero, so it would never evict ->
 * unbounded growth / OOM.
 */
@RunWith(RobolectricTestRunner::class)
class ImagePipelineFactoryNoopGcSafetyTest {

  @After
  fun tearDown() {
    // Reset the global so other tests are unaffected.
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
  }

  @Test
  fun isNoopWithNonGcSafeFactory_whenNoopAndNonGcSafeFactory_thenTrue() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_NOOP)
    val factory = CountingLruBitmapMemoryCacheFactory()

    assertThat(factory.isGcSafe()).isFalse()
    assertThat(ImagePipelineFactory.isNoopWithNonGcSafeFactory(factory)).isTrue()
  }

  @Test
  fun isNoopWithNonGcSafeFactory_whenNoopAndGcSafeFactory_thenFalse() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_NOOP)
    val factory = GcSafeBitmapMemoryCacheFactory()

    assertThat(factory.isGcSafe()).isTrue()
    assertThat(ImagePipelineFactory.isNoopWithNonGcSafeFactory(factory)).isFalse()
  }

  @Test
  fun isNoopWithNonGcSafeFactory_whenDefaultAndNonGcSafeFactory_thenFalse() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
    val factory = CountingLruBitmapMemoryCacheFactory()

    assertThat(ImagePipelineFactory.isNoopWithNonGcSafeFactory(factory)).isFalse()
  }

  @Test
  fun isNoopWithNonGcSafeFactory_whenDefaultAndGcSafeFactory_thenFalse() {
    CloseableReference.setDisableCloseableReferencesForBitmaps(CloseableReference.REF_TYPE_DEFAULT)
    val factory = GcSafeBitmapMemoryCacheFactory()

    assertThat(ImagePipelineFactory.isNoopWithNonGcSafeFactory(factory)).isFalse()
  }

  /** A stand-in GC-safe factory; only [isGcSafe] matters for the predicate under test. */
  private class GcSafeBitmapMemoryCacheFactory : BitmapMemoryCacheFactory {
    override fun create(
        bitmapMemoryCacheParamsSupplier: Supplier<MemoryCacheParams>,
        memoryTrimmableRegistry: MemoryTrimmableRegistry,
        trimStrategy: MemoryCache.CacheTrimStrategy,
        storeEntrySize: Boolean,
        ignoreSizeMismatch: Boolean,
        observer: CountingMemoryCache.EntryStateObserver<CacheKey>?,
    ): CountingMemoryCache<CacheKey, CloseableImage> {
      throw UnsupportedOperationException("not needed for this test")
    }

    override fun isGcSafe(): Boolean = true
  }
}
