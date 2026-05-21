/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.internal.Predicate
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoutingBitmapMemoryCacheTest {

  private lateinit var bitmapCache: MemoryCache<CacheKey, CloseableImage>
  private lateinit var nonBitmapCache: MemoryCache<CacheKey, CloseableImage>
  private lateinit var router: RoutingBitmapMemoryCache
  private lateinit var key: CacheKey
  private lateinit var bitmapImage: CloseableImage
  private lateinit var animatedImage: CloseableImage
  private lateinit var bitmapRef: CloseableReference<CloseableImage>
  private lateinit var animatedRef: CloseableReference<CloseableImage>

  @Before
  fun setUp() {
    bitmapCache = mock()
    nonBitmapCache = mock()
    router = RoutingBitmapMemoryCache(bitmapCache, nonBitmapCache)
    key = SimpleCacheKey("http://example.com/img.png")

    // CloseableBitmap routes to the bitmap cache.
    bitmapImage = mock<CloseableBitmap>()
    bitmapRef = mock()
    whenever(bitmapRef.get()).thenReturn(bitmapImage)

    // Plain CloseableImage (not CloseableBitmap) routes to the non-bitmap cache.
    animatedImage = mock()
    animatedRef = mock()
    whenever(animatedRef.get()).thenReturn(animatedImage)
  }

  @Test
  fun cache_bitmapValue_writesToBitmapCache() {
    router.cache(key, bitmapRef)

    verify(bitmapCache).cache(eq(key), eq(bitmapRef))
    verify(nonBitmapCache, never()).cache(any(), any())
  }

  @Test
  fun cache_nonBitmapValue_writesToNonBitmapCache() {
    router.cache(key, animatedRef)

    verify(nonBitmapCache).cache(eq(key), eq(animatedRef))
    verify(bitmapCache, never()).cache(any(), any())
  }

  @Test
  fun cache_nullValue_isRejected() {
    val nullRef = mock<CloseableReference<CloseableImage>>()
    whenever(nullRef.get()).thenReturn(null)

    assertThat(router.cache(key, nullRef)).isNull()
    verify(bitmapCache, never()).cache(any(), any())
    verify(nonBitmapCache, never()).cache(any(), any())
  }

  @Test
  fun get_hitsBitmapCacheFirst() {
    whenever(bitmapCache.get(key)).thenReturn(bitmapRef)

    val result = router.get(key)

    assertThat(result).isSameAs(bitmapRef)
    verify(nonBitmapCache, never()).get(any())
  }

  @Test
  fun get_fallsThroughToNonBitmapCacheOnBitmapMiss() {
    whenever(bitmapCache.get(key)).thenReturn(null)
    whenever(nonBitmapCache.get(key)).thenReturn(animatedRef)

    val result = router.get(key)

    assertThat(result).isSameAs(animatedRef)
    verify(bitmapCache).get(key)
    verify(nonBitmapCache).get(key)
  }

  @Test
  fun get_returnsNullWhenBothMiss() {
    whenever(bitmapCache.get(key)).thenReturn(null)
    whenever(nonBitmapCache.get(key)).thenReturn(null)

    assertThat(router.get(key)).isNull()
  }

  @Test
  fun inspect_fallsThroughToNonBitmapCache() {
    whenever(bitmapCache.inspect(key)).thenReturn(null)
    whenever(nonBitmapCache.inspect(key)).thenReturn(animatedImage)

    assertThat(router.inspect(key)).isSameAs(animatedImage)
  }

  @Test
  fun probe_touchesBothCaches() {
    router.probe(key)

    verify(bitmapCache).probe(key)
    verify(nonBitmapCache).probe(key)
  }

  @Test
  fun removeAll_sumsBothCaches() {
    val predicate = Predicate<CacheKey> { true }
    whenever(bitmapCache.removeAll(predicate)).thenReturn(3)
    whenever(nonBitmapCache.removeAll(predicate)).thenReturn(2)

    assertThat(router.removeAll(predicate)).isEqualTo(5)
  }

  @Test
  fun containsKey_shortCircuitsOnBitmapHit() {
    whenever(bitmapCache.contains(key)).thenReturn(true)

    assertThat(router.contains(key)).isTrue()
    verify(nonBitmapCache, never()).contains(any<CacheKey>())
  }

  @Test
  fun containsKey_checksNonBitmapOnBitmapMiss() {
    whenever(bitmapCache.contains(key)).thenReturn(false)
    whenever(nonBitmapCache.contains(key)).thenReturn(true)

    assertThat(router.contains(key)).isTrue()
  }

  @Test
  fun containsPredicate_shortCircuitsOnBitmapHit() {
    val predicate = Predicate<CacheKey> { true }
    whenever(bitmapCache.contains(predicate)).thenReturn(true)

    assertThat(router.contains(predicate)).isTrue()
    verify(nonBitmapCache, never()).contains(any<Predicate<CacheKey>>())
  }

  @Test
  fun count_sumsBothCaches() {
    whenever(bitmapCache.count).thenReturn(4)
    whenever(nonBitmapCache.count).thenReturn(7)

    assertThat(router.count).isEqualTo(11)
  }

  @Test
  fun sizeInBytes_sumsBothCaches() {
    whenever(bitmapCache.sizeInBytes).thenReturn(1_024)
    whenever(nonBitmapCache.sizeInBytes).thenReturn(2_048)

    assertThat(router.sizeInBytes).isEqualTo(3_072)
  }

  @Test
  fun trim_fansOutToBothCaches() {
    router.trim(MemoryTrimType.OnAppBackgrounded)

    verify(bitmapCache).trim(MemoryTrimType.OnAppBackgrounded)
    verify(nonBitmapCache).trim(MemoryTrimType.OnAppBackgrounded)
  }
}
