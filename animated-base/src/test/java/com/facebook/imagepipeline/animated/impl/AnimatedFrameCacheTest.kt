/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.impl

import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.references.CloseableReference
import com.facebook.common.util.ByteConstants
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.cache.BitmapMemoryCacheTrimStrategy
import com.facebook.imagepipeline.cache.CountingLruBitmapMemoryCacheFactory
import com.facebook.imagepipeline.cache.CountingMemoryCache
import com.facebook.imagepipeline.cache.MemoryCacheParams
import com.facebook.imagepipeline.image.CloseableImage
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimatedFrameCacheTest {
  private lateinit var memoryTrimmableRegistry: MemoryTrimmableRegistry
  private lateinit var memoryCacheParamsSupplier: Supplier<MemoryCacheParams>
  private lateinit var platformBitmapFactory: PlatformBitmapFactory

  private lateinit var cacheKey: CacheKey
  private lateinit var animatedFrameCache: AnimatedFrameCache
  private lateinit var frame1: CloseableReference<CloseableImage>
  private lateinit var frame2: CloseableReference<CloseableImage>

  @Before
  fun setUp() {
    memoryTrimmableRegistry = mock()
    memoryCacheParamsSupplier = mock()
    platformBitmapFactory = mock()

    val params: MemoryCacheParams =
        MemoryCacheParams(
            4 * ByteConstants.MB,
            256,
            Int.Companion.MAX_VALUE,
            Int.Companion.MAX_VALUE,
            Int.Companion.MAX_VALUE,
            TimeUnit.MINUTES.toMillis(5))
    whenever(memoryCacheParamsSupplier.get()).thenReturn(params)
    val countingMemoryCache: CountingMemoryCache<CacheKey, CloseableImage> =
        CountingLruBitmapMemoryCacheFactory()
            .create(
                memoryCacheParamsSupplier,
                memoryTrimmableRegistry,
                BitmapMemoryCacheTrimStrategy(),
                false,
                false,
                null)
    cacheKey = SimpleCacheKey("key")
    animatedFrameCache = AnimatedFrameCache(cacheKey, countingMemoryCache)
    frame1 = CloseableReference.of(mock<CloseableImage>())
    frame2 = CloseableReference.of(mock<CloseableImage>())
  }

  @Test
  fun testBasic() {
    val ret = animatedFrameCache.cache(1, frame1)
    assertThat(ret?.get()).isSameAs(frame1.get())
  }

  @Test
  fun testMultipleFrames() {
    animatedFrameCache.cache(1, frame1)
    animatedFrameCache.cache(2, frame2)
    assertThat(animatedFrameCache.get(1)?.get()).isSameAs(frame1.get())
    assertThat(animatedFrameCache.get(2)?.get()).isSameAs(frame2.get())
  }

  @Test
  fun testReplace() {
    animatedFrameCache.cache(1, frame1)
    animatedFrameCache.cache(1, frame2)
    assertThat(animatedFrameCache.get(1)?.get()).isNotSameAs(frame1.get())
    assertThat(animatedFrameCache.get(1)?.get()).isSameAs(frame2.get())
  }

  @Test
  fun testReuse() {
    val ret = animatedFrameCache.cache(1, frame1)
    ret?.close()
    val free = animatedFrameCache.getForReuse()
    assertThat(free).isNotNull()
  }

  @Test
  fun testCantReuseIfNotClosed() {
    val ret = animatedFrameCache.cache(1, frame1)
    val free = animatedFrameCache.getForReuse()
    assertThat(free).isNull()
  }

  @Test
  fun testStillThereIfClosed() {
    val ret = animatedFrameCache.cache(1, frame1)
    ret?.close()
    assertThat(animatedFrameCache.get(1)).isNotNull()
  }

  @Test
  fun testContains() {
    assertThat(animatedFrameCache.contains(1)).isFalse()

    val ret = animatedFrameCache.cache(1, frame1)

    assertThat(animatedFrameCache.contains(1)).isTrue()
    assertThat(animatedFrameCache.contains(2)).isFalse()

    ret?.close()

    assertThat(animatedFrameCache.contains(1)).isTrue()
    assertThat(animatedFrameCache.contains(2)).isFalse()
  }

  @Test
  fun testContainsWhenReused() {
    val ret = animatedFrameCache.cache(1, frame1)
    ret?.close()

    assertThat(animatedFrameCache.contains(1)).isTrue()
    assertThat(animatedFrameCache.contains(2)).isFalse()

    val free = animatedFrameCache.getForReuse()
    free?.close()

    assertThat(animatedFrameCache.contains(1)).isFalse()
    assertThat(animatedFrameCache.contains(2)).isFalse()
  }

  @Test
  fun testContainsFullReuseFlowWithMultipleItems() {
    assertThat(animatedFrameCache.contains(1)).isFalse()
    assertThat(animatedFrameCache.contains(2)).isFalse()

    val ret = animatedFrameCache.cache(1, frame1)
    val ret2 = animatedFrameCache.cache(2, frame2)

    assertThat(animatedFrameCache.contains(1)).isTrue()
    assertThat(animatedFrameCache.contains(2)).isTrue()

    ret?.close()
    ret2?.close()

    assertThat(animatedFrameCache.contains(1)).isTrue()
    assertThat(animatedFrameCache.contains(2)).isTrue()

    var free = animatedFrameCache.getForReuse()
    free?.close()

    assertThat(animatedFrameCache.contains(1)).isFalse()
    assertThat(animatedFrameCache.contains(2)).isTrue()

    free = animatedFrameCache.getForReuse()
    free?.close()

    assertThat(animatedFrameCache.contains(1)).isFalse()
    assertThat(animatedFrameCache.contains(2)).isFalse()
  }
}
