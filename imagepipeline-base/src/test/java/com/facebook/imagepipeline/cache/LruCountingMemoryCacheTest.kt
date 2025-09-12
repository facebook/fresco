/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import android.graphics.Bitmap
import android.os.SystemClock
import com.facebook.common.internal.Predicate
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LruCountingMemoryCacheTest {

  companion object {
    private const val CACHE_MAX_SIZE = 1200
    private const val CACHE_MAX_COUNT = 4
    private const val CACHE_EVICTION_QUEUE_MAX_SIZE = 1100
    private const val CACHE_EVICTION_QUEUE_MAX_COUNT = 3
    private const val CACHE_ENTRY_MAX_SIZE = 1000
    private val PARAMS_CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5)

    private const val KEY = "KEY"
    private val KEYS = arrayOf("k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9")

    private val FAKE_BITMAP_RESOURCE_RELEASER =
        object : ResourceReleaser<Bitmap> {
          override fun release(value: Bitmap) {}
        }
  }

  @Mock lateinit var releaser: ResourceReleaser<Int>

  @Mock lateinit var cacheTrimStrategy: MemoryCache.CacheTrimStrategy

  @Mock lateinit var paramsSupplier: Supplier<MemoryCacheParams>

  @Mock lateinit var entryStateObserver: CountingMemoryCache.EntryStateObserver<String>

  @Mock lateinit var bitmap: Bitmap

  private lateinit var valueDescriptor: ValueDescriptor<Int>
  private lateinit var params: MemoryCacheParams
  private lateinit var cache: LruCountingMemoryCache<String, Int>
  private lateinit var mockedSystemClock: MockedStatic<SystemClock>

  @Before
  fun setUp() {
    mockedSystemClock = mockStatic(SystemClock::class.java)
    MockitoAnnotations.initMocks(this)
    mockedSystemClock.`when`<Long> { SystemClock.uptimeMillis() }.thenReturn(0L)

    valueDescriptor =
        object : ValueDescriptor<Int> {
          override fun getSizeInBytes(value: Int): Int = value
        }

    params =
        MemoryCacheParams(
            CACHE_MAX_SIZE,
            CACHE_MAX_COUNT,
            CACHE_EVICTION_QUEUE_MAX_SIZE,
            CACHE_EVICTION_QUEUE_MAX_COUNT,
            CACHE_ENTRY_MAX_SIZE,
            PARAMS_CHECK_INTERVAL_MS,
        )

    `when`(paramsSupplier.get()).thenReturn(params)
    cache =
        LruCountingMemoryCache(
            valueDescriptor,
            cacheTrimStrategy,
            paramsSupplier,
            null,
            false,
            false,
        )
  }

  @After
  fun tearDownStaticMocks() {
    mockedSystemClock.close()
  }

  @Test
  fun testCache() {
    cache.cache(KEY, newReference(100))
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 1)
    verify(releaser, never()).release(anyInt())
  }

  @Test
  fun testClosingOriginalReference() {
    val originalRef = newReference(100)
    cache.cache(KEY, originalRef)
    // cache should make its own copy and closing the original reference after caching
    // should not affect the cached value
    originalRef.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 1)
    verify(releaser, never()).release(anyInt())
  }

  @Test
  fun testClosingClientReference() {
    val cachedRef = cache.cache(KEY, newReference(100))
    // cached item should get exclusively owned
    cachedRef?.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(1, 100)
    assertExclusivelyOwned(KEY, 100)
    verify(releaser, never()).release(anyInt())
  }

  @Test
  fun testNotExclusiveAtFirst() {
    cache.cache(KEY, newReference(100), entryStateObserver)
    verify(entryStateObserver, never()).onExclusivityChanged(anyString(), anyBoolean())
  }

  @Test
  fun testToggleExclusive() {
    val cachedRef = cache.cache(KEY, newReference(100), entryStateObserver)
    cachedRef?.close()
    verify(entryStateObserver).onExclusivityChanged(KEY, true)
    cache.get(KEY)
    verify(entryStateObserver).onExclusivityChanged(KEY, false)
  }

  @Test
  fun testCantReuseNonExclusive() {
    val cachedRef = cache.cache(KEY, newReference(100), entryStateObserver)
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    val reusedRef = cache.reuse(KEY)
    assertThat(reusedRef).isNull()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    verify(entryStateObserver, never()).onExclusivityChanged(anyString(), anyBoolean())
    cachedRef?.close()
  }

  @Test
  fun testCanReuseExclusive() {
    var cachedRef = cache.cache(KEY, newReference(100), entryStateObserver)
    cachedRef?.close()
    verify(entryStateObserver).onExclusivityChanged(KEY, true)
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(1, 100)
    cachedRef = cache.reuse(KEY)
    assertThat(cachedRef).isNotNull()
    verify(entryStateObserver).onExclusivityChanged(KEY, false)
    assertTotalSize(0, 0)
    assertExclusivelyOwnedSize(0, 0)
    cachedRef?.close()
    verify(entryStateObserver).onExclusivityChanged(KEY, true)
  }

  @Test
  fun testReuseExclusive_CacheSameItem() {
    var cachedRef = cache.cache(KEY, newReference(100), entryStateObserver)
    cachedRef?.close()
    verify(entryStateObserver).onExclusivityChanged(KEY, true)
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(1, 100)
    cachedRef = cache.reuse(KEY)
    assertThat(cachedRef).isNotNull()
    verify(entryStateObserver).onExclusivityChanged(KEY, false)
    assertTotalSize(0, 0)
    assertExclusivelyOwnedSize(0, 0)
    cachedRef?.let { ref ->
      val newItem = cache.cache(KEY, ref)
      ref.close()
      assertTotalSize(1, 100)
      assertExclusivelyOwnedSize(0, 0)
      newItem?.close()
      verify(entryStateObserver).onExclusivityChanged(KEY, true)
      assertTotalSize(1, 100)
      assertExclusivelyOwnedSize(1, 100)
    }
  }

  @Test
  fun testReuseExclusive_CacheSameItemWithDifferentKey() {
    var cachedRef = cache.cache(KEY, newReference(100), entryStateObserver)
    cachedRef?.close()
    verify(entryStateObserver).onExclusivityChanged(KEY, true)
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(1, 100)
    cachedRef = cache.reuse(KEY)
    assertThat(cachedRef).isNotNull()
    verify(entryStateObserver).onExclusivityChanged(KEY, false)
    assertTotalSize(0, 0)
    assertExclusivelyOwnedSize(0, 0)
    cachedRef?.let { ref ->
      val newItem = cache.cache(KEYS[2], ref)
      ref.close()
      assertTotalSize(1, 100)
      assertExclusivelyOwnedSize(0, 0)
      newItem?.close()
      verify(entryStateObserver).onExclusivityChanged(KEY, true)
      assertTotalSize(1, 100)
      assertExclusivelyOwnedSize(1, 100)
    }
  }

  @Test
  fun testInUseCount() {
    val cachedRef1 = cache.cache(KEY, newReference(100))

    val cachedRef2a = cache.get(KEY)
    val cachedRef2b = cachedRef2a?.clone()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 2)

    val cachedRef3a = cache.get(KEY)
    val cachedRef3b = cachedRef3a?.clone()
    val cachedRef3c = cachedRef3b?.clone()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 3)

    cachedRef1?.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 2)

    // all copies of cachedRef2a need to be closed for usage count to drop
    cachedRef2a?.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 2)
    cachedRef2b?.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 1)

    // all copies of cachedRef3a need to be closed for usage count to drop
    cachedRef3c?.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 1)
    cachedRef3b?.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEY, 100, 1)
    cachedRef3a?.close()
    assertTotalSize(1, 100)
    assertExclusivelyOwnedSize(1, 100)
    assertExclusivelyOwned(KEY, 100)
  }

  @Test
  fun testCachingSameKeyTwice() {
    val originalRef1 = newReference(110)
    val cachedRef1 = cache.cache(KEY, originalRef1)
    val cachedRef2a = cache.get(KEY)
    val cachedRef2b = cachedRef2a?.clone()
    val cachedRef3 = cache.get(KEY)
    val entry1 = cache.mCachedEntries[KEY]

    val cachedRef2 = cache.cache(KEY, newReference(120))
    val entry2 = cache.mCachedEntries[KEY]
    assertThat(entry1).isNotSameAs(entry2)
    entry1?.let { assertOrphanWithCount(it, 3) }
    assertSharedWithCount(KEY, 120, 1)

    // release the orphaned reference only when all clients are gone
    originalRef1.close()
    cachedRef2b?.close()
    entry1?.let { assertOrphanWithCount(it, 3) }
    cachedRef2a?.close()
    entry1?.let { assertOrphanWithCount(it, 2) }
    cachedRef1?.close()
    entry1?.let { assertOrphanWithCount(it, 1) }
    verify(releaser, never()).release(anyInt())
    cachedRef3?.close()
    entry1?.let { assertOrphanWithCount(it, 0) }
    verify(releaser).release(110)
  }

  @Test
  fun testDoesNotCacheBigValues() {
    assertThat(cache.cache(KEY, newReference(CACHE_ENTRY_MAX_SIZE + 1))).isNull()
  }

  @Test
  fun testDoesCacheNotTooBigValues() {
    assertThat(cache.cache(KEY, newReference(CACHE_ENTRY_MAX_SIZE))).isNotNull()
  }

  @Test
  fun testEviction_ByTotalSize() {
    // value 4 cannot fit the cache
    val originalRef1 = newReference(400)
    val valueRef1 = cache.cache(KEYS[1], originalRef1)
    originalRef1.close()
    val originalRef2 = newReference(500)
    val valueRef2 = cache.cache(KEYS[2], originalRef2)
    originalRef2.close()
    val originalRef3 = newReference(100)
    val valueRef3 = cache.cache(KEYS[3], originalRef3)
    originalRef3.close()
    val originalRef4 = newReference(700)
    val valueRef4 = cache.cache(KEYS[4], originalRef4)
    originalRef4.close()
    assertTotalSize(3, 1000)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEYS[1], 400, 1)
    assertSharedWithCount(KEYS[2], 500, 1)
    assertSharedWithCount(KEYS[3], 100, 1)
    assertNotCached(KEYS[4], 700)
    assertThat(valueRef4).isNull()

    // closing the clients of cached items will make them viable for eviction
    valueRef1?.close()
    valueRef2?.close()
    valueRef3?.close()
    assertTotalSize(3, 1000)
    assertExclusivelyOwnedSize(3, 1000)

    // value 4 can now fit after evicting value1 and value2
    cache.cache(KEYS[4], newReference(700))
    assertTotalSize(2, 800)
    assertExclusivelyOwnedSize(1, 100)
    assertNotCached(KEYS[1], 400)
    assertNotCached(KEYS[2], 500)
    assertExclusivelyOwned(KEYS[3], 100)
    assertSharedWithCount(KEYS[4], 700, 1)
    verify(releaser).release(400)
    verify(releaser).release(500)
  }

  @Test
  fun testEviction_ByTotalCount() {
    // value 5 cannot fit the cache
    val originalRef1 = newReference(110)
    val valueRef1 = cache.cache(KEYS[1], originalRef1)
    originalRef1.close()
    val originalRef2 = newReference(120)
    val valueRef2 = cache.cache(KEYS[2], originalRef2)
    originalRef2.close()
    val originalRef3 = newReference(130)
    val valueRef3 = cache.cache(KEYS[3], originalRef3)
    originalRef3.close()
    val originalRef4 = newReference(140)
    val valueRef4 = cache.cache(KEYS[4], originalRef4)
    originalRef4.close()
    val originalRef5 = newReference(150)
    val valueRef5 = cache.cache(KEYS[5], originalRef5)
    originalRef5.close()
    assertTotalSize(4, 500)
    assertExclusivelyOwnedSize(0, 0)
    assertSharedWithCount(KEYS[1], 110, 1)
    assertSharedWithCount(KEYS[2], 120, 1)
    assertSharedWithCount(KEYS[3], 130, 1)
    assertSharedWithCount(KEYS[4], 140, 1)
    assertNotCached(KEYS[5], 150)
    assertThat(valueRef5).isNull()

    // closing the clients of cached items will make them viable for eviction
    valueRef1?.close()
    valueRef2?.close()
    valueRef3?.close()
    assertTotalSize(4, 500)
    assertExclusivelyOwnedSize(3, 360)

    // value 4 can now fit after evicting value1
    cache.cache(KEYS[5], newReference(150))
    assertTotalSize(4, 540)
    assertExclusivelyOwnedSize(2, 250)
    assertNotCached(KEYS[1], 110)
    assertExclusivelyOwned(KEYS[2], 120)
    assertExclusivelyOwned(KEYS[3], 130)
    assertSharedWithCount(KEYS[4], 140, 1)
    assertSharedWithCount(KEYS[5], 150, 1)
    verify(releaser).release(110)
  }

  @Test
  fun testEviction_ByEvictionQueueSize() {
    val originalRef1 = newReference(200)
    val valueRef1 = cache.cache(KEYS[1], originalRef1)
    originalRef1.close()
    valueRef1?.close()
    val originalRef2 = newReference(300)
    val valueRef2 = cache.cache(KEYS[2], originalRef2)
    originalRef2.close()
    valueRef2?.close()
    val originalRef3 = newReference(700)
    val valueRef3 = cache.cache(KEYS[3], originalRef3)
    originalRef3.close()
    assertTotalSize(3, 1200)
    assertExclusivelyOwnedSize(2, 500)
    assertExclusivelyOwned(KEYS[1], 200)
    assertExclusivelyOwned(KEYS[2], 300)
    assertSharedWithCount(KEYS[3], 700, 1)
    verify(releaser, never()).release(anyInt())

    // closing the client reference for item3 will cause item1 to be evicted
    valueRef3?.close()
    assertTotalSize(2, 1000)
    assertExclusivelyOwnedSize(2, 1000)
    assertNotCached(KEYS[1], 200)
    assertExclusivelyOwned(KEYS[2], 300)
    assertExclusivelyOwned(KEYS[3], 700)
    verify(releaser).release(200)
  }

  @Test
  fun testEviction_ByEvictionQueueCount() {
    val originalRef1 = newReference(110)
    val valueRef1 = cache.cache(KEYS[1], originalRef1)
    originalRef1.close()
    valueRef1?.close()
    val originalRef2 = newReference(120)
    val valueRef2 = cache.cache(KEYS[2], originalRef2)
    originalRef2.close()
    valueRef2?.close()
    val originalRef3 = newReference(130)
    val valueRef3 = cache.cache(KEYS[3], originalRef3)
    originalRef3.close()
    valueRef3?.close()
    val originalRef4 = newReference(140)
    val valueRef4 = cache.cache(KEYS[4], originalRef4)
    originalRef4.close()
    assertTotalSize(4, 500)
    assertExclusivelyOwnedSize(3, 360)
    assertExclusivelyOwned(KEYS[1], 110)
    assertExclusivelyOwned(KEYS[2], 120)
    assertExclusivelyOwned(KEYS[3], 130)
    assertSharedWithCount(KEYS[4], 140, 1)
    verify(releaser, never()).release(anyInt())

    // closing the client reference for item4 will cause item1 to be evicted
    valueRef4?.close()
    assertTotalSize(3, 390)
    assertExclusivelyOwnedSize(3, 390)
    assertNotCached(KEYS[1], 110)
    assertExclusivelyOwned(KEYS[2], 120)
    assertExclusivelyOwned(KEYS[3], 130)
    assertExclusivelyOwned(KEYS[4], 140)
    verify(releaser).release(110)
  }

  @Test
  fun testUpdatesCacheParams() {
    val inOrder = inOrder(paramsSupplier)

    val originalRef = newReference(700)
    val cachedRef = cache.cache(KEYS[2], originalRef)
    originalRef.close()
    cachedRef?.close()

    cache.get(KEY)
    inOrder.verify(paramsSupplier).get()

    mockedSystemClock
        .`when`<Long> { SystemClock.uptimeMillis() }
        .thenReturn(PARAMS_CHECK_INTERVAL_MS - 1)
    cache.get(KEY)
    inOrder.verify(paramsSupplier, never()).get()
    cache.get(KEY)
    inOrder.verify(paramsSupplier, never()).get()

    assertTotalSize(1, 700)
    assertExclusivelyOwnedSize(1, 700)

    params =
        MemoryCacheParams(
            500, // cache max size
            CACHE_MAX_COUNT,
            CACHE_EVICTION_QUEUE_MAX_SIZE,
            CACHE_EVICTION_QUEUE_MAX_COUNT,
            CACHE_ENTRY_MAX_SIZE,
            PARAMS_CHECK_INTERVAL_MS,
        )
    `when`(paramsSupplier.get()).thenReturn(params)

    mockedSystemClock
        .`when`<Long> { SystemClock.uptimeMillis() }
        .thenReturn(PARAMS_CHECK_INTERVAL_MS)
    cache.get(KEY)
    inOrder.verify(paramsSupplier).get()

    assertTotalSize(0, 0)
    assertExclusivelyOwnedSize(0, 0)
    verify(releaser).release(700)
  }

  @Test
  fun testRemoveAllMatchingPredicate() {
    val originalRef1 = newReference(110)
    val valueRef1 = cache.cache(KEYS[1], originalRef1)
    originalRef1.close()
    valueRef1?.close()
    val originalRef2 = newReference(120)
    val valueRef2 = cache.cache(KEYS[2], originalRef2)
    originalRef2.close()
    valueRef2?.close()
    val originalRef3 = newReference(130)
    val valueRef3 = cache.cache(KEYS[3], originalRef3)
    originalRef3.close()
    val entry3 = cache.mCachedEntries[KEYS[3]]
    val originalRef4 = newReference(150)
    val valueRef4 = cache.cache(KEYS[4], originalRef4)
    originalRef4.close()

    val numEvictedEntries =
        cache.removeAll(
            object : Predicate<String> {
              override fun apply(key: String): Boolean {
                return key == KEYS[2] || key == KEYS[3]
              }
            }
        )

    assertThat(numEvictedEntries).isEqualTo(2)

    assertTotalSize(2, 260)
    assertExclusivelyOwnedSize(1, 110)
    assertExclusivelyOwned(KEYS[1], 110)
    assertNotCached(KEYS[2], 120)
    entry3?.let { assertOrphanWithCount(it, 1) }
    assertSharedWithCount(KEYS[4], 150, 1)

    verify(releaser).release(120)
    verify(releaser, never()).release(130)

    valueRef3?.close()
    verify(releaser).release(130)
  }

  @Test
  fun testClear() {
    val originalRef1 = newReference(110)
    val cachedRef1 = cache.cache(KEYS[1], originalRef1)
    originalRef1.close()
    val entry1 = cache.mCachedEntries[KEYS[1]]
    val originalRef2 = newReference(120)
    val cachedRef2 = cache.cache(KEYS[2], originalRef2)
    originalRef2.close()
    cachedRef2?.close()

    cache.clear()
    assertTotalSize(0, 0)
    assertExclusivelyOwnedSize(0, 0)
    entry1?.let { assertOrphanWithCount(it, 1) }
    assertNotCached(KEYS[2], 120)
    verify(releaser).release(120)

    cachedRef1?.close()
    verify(releaser).release(110)
  }

  @Test
  fun testTrimming() {
    val memoryTrimType = MemoryTrimType.OnCloseToDalvikHeapLimit
    params = MemoryCacheParams(1100, 10, 1100, 10, 110, PARAMS_CHECK_INTERVAL_MS)
    `when`(paramsSupplier.get()).thenReturn(params)
    mockedSystemClock
        .`when`<Long> { SystemClock.uptimeMillis() }
        .thenReturn(PARAMS_CHECK_INTERVAL_MS)
    val inOrder = inOrder(releaser)

    // create original references
    val originalRefs = Array<CloseableReference<Int>?>(10) { null }
    for (i in 0 until 10) {
      originalRefs[i] = newReference(100 + i)
    }
    // cache items & close the original references
    val cachedRefs = Array<CloseableReference<Int>?>(10) { null }
    for (i in 0 until 10) {
      originalRefs[i]?.let { ref -> cachedRefs[i] = cache.cache(KEYS[i], ref) }
      originalRefs[i]?.close()
    }
    // cache should keep alive the items until evicted
    inOrder.verify(releaser, never()).release(anyInt())

    // trimming cannot evict shared entries
    `when`(cacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(1.00)
    cache.trim(memoryTrimType)
    assertSharedWithCount(KEYS[0], 100, 1)
    assertSharedWithCount(KEYS[1], 101, 1)
    assertSharedWithCount(KEYS[2], 102, 1)
    assertSharedWithCount(KEYS[3], 103, 1)
    assertSharedWithCount(KEYS[4], 104, 1)
    assertSharedWithCount(KEYS[5], 105, 1)
    assertSharedWithCount(KEYS[6], 106, 1)
    assertSharedWithCount(KEYS[7], 107, 1)
    assertSharedWithCount(KEYS[8], 108, 1)
    assertSharedWithCount(KEYS[9], 109, 1)
    assertTotalSize(10, 1045)
    assertExclusivelyOwnedSize(0, 0)

    // close 7 client references
    cachedRefs[8]?.close()
    cachedRefs[2]?.close()
    cachedRefs[7]?.close()
    cachedRefs[3]?.close()
    cachedRefs[6]?.close()
    cachedRefs[4]?.close()
    cachedRefs[5]?.close()
    assertSharedWithCount(KEYS[0], 100, 1)
    assertSharedWithCount(KEYS[1], 101, 1)
    assertSharedWithCount(KEYS[9], 109, 1)
    assertExclusivelyOwned(KEYS[8], 108)
    assertExclusivelyOwned(KEYS[2], 102)
    assertExclusivelyOwned(KEYS[7], 107)
    assertExclusivelyOwned(KEYS[3], 103)
    assertExclusivelyOwned(KEYS[6], 106)
    assertExclusivelyOwned(KEYS[4], 104)
    assertExclusivelyOwned(KEYS[5], 105)
    assertTotalSize(10, 1045)
    assertExclusivelyOwnedSize(7, 735)

    // Trim cache by 45%. This means that out of total of 1045 bytes cached, 574 should remain.
    // 310 bytes is used by the clients, which leaves 264 for the exclusively owned items.
    // Only the two most recent exclusively owned items fit, and they occupy 209 bytes.
    `when`(cacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(0.45)
    cache.trim(memoryTrimType)
    assertSharedWithCount(KEYS[0], 100, 1)
    assertSharedWithCount(KEYS[1], 101, 1)
    assertSharedWithCount(KEYS[9], 109, 1)
    assertExclusivelyOwned(KEYS[4], 104)
    assertExclusivelyOwned(KEYS[5], 105)
    assertNotCached(KEYS[8], 108)
    assertNotCached(KEYS[2], 102)
    assertNotCached(KEYS[7], 107)
    assertNotCached(KEYS[3], 103)
    assertNotCached(KEYS[6], 106)
    assertTotalSize(5, 519)
    assertExclusivelyOwnedSize(2, 209)
    inOrder.verify(releaser).release(108)
    inOrder.verify(releaser).release(102)
    inOrder.verify(releaser).release(107)
    inOrder.verify(releaser).release(103)
    inOrder.verify(releaser).release(106)

    // Full trim. All exclusively owned items should be evicted.
    `when`(cacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(1.00)
    cache.trim(memoryTrimType)
    assertSharedWithCount(KEYS[0], 100, 1)
    assertSharedWithCount(KEYS[1], 101, 1)
    assertSharedWithCount(KEYS[9], 109, 1)
    assertNotCached(KEYS[8], 108)
    assertNotCached(KEYS[2], 102)
    assertNotCached(KEYS[7], 107)
    assertNotCached(KEYS[3], 103)
    assertNotCached(KEYS[6], 106)
    assertNotCached(KEYS[6], 104)
    assertNotCached(KEYS[6], 105)
    assertTotalSize(3, 310)
    assertExclusivelyOwnedSize(0, 0)
    inOrder.verify(releaser).release(104)
    inOrder.verify(releaser).release(105)
  }

  @Test
  fun testContains() {
    assertThat(cache.contains(KEY)).isFalse()

    val newRef = cache.cache(KEY, newReference(100))

    assertThat(cache.contains(KEY)).isTrue()
    assertThat(cache.contains(KEYS[0])).isFalse()

    newRef?.close()

    assertThat(cache.contains(KEY)).isTrue()
    assertThat(cache.contains(KEYS[0])).isFalse()

    val reuse = cache.reuse(KEY)
    reuse?.close()

    assertThat(cache.contains(KEY)).isFalse()
    assertThat(cache.contains(KEYS[0])).isFalse()
  }

  private fun newReference(size: Int): CloseableReference<Int> {
    return CloseableReference.of(size, releaser)
  }

  private fun assertSharedWithCount(key: String, value: Int, count: Int) {
    assertThat(cache.mCachedEntries.contains(key))
        .describedAs("key not found in the cache")
        .isTrue()
    assertThat(cache.mExclusiveEntries.contains(key))
        .describedAs("key found in the exclusives")
        .isFalse()
    val entry = cache.mCachedEntries[key]
    assertThat(entry).describedAs("entry not found in the cache").isNotNull()
    entry?.let {
      assertThat(it.key).describedAs("key mismatch").isEqualTo(key)
      assertThat(it.valueRef.get()).describedAs("value mismatch").isEqualTo(value)
      assertThat(it.clientCount).describedAs("client count mismatch").isEqualTo(count)
      assertThat(it.isOrphan).describedAs("entry is an orphan").isFalse()
    }
  }

  private fun assertExclusivelyOwned(key: String, value: Int) {
    assertThat(cache.mCachedEntries.contains(key))
        .describedAs("key not found in the cache")
        .isTrue()
    assertThat(cache.mExclusiveEntries.contains(key))
        .describedAs("key not found in the exclusives")
        .isTrue()
    val entry = cache.mCachedEntries[key]
    assertThat(entry).describedAs("entry not found in the cache").isNotNull()
    entry?.let { cacheEntry ->
      assertThat(cacheEntry.key).describedAs("key mismatch").isEqualTo(key)
      assertThat(cacheEntry.valueRef.get()).describedAs("value mismatch").isEqualTo(value)
      assertThat(cacheEntry.clientCount).describedAs("client count greater than zero").isEqualTo(0)
      assertThat(cacheEntry.isOrphan).describedAs("entry is an orphan").isFalse()
    }
  }

  private fun assertNotCached(key: String, value: Int) {
    assertThat(cache.mCachedEntries.contains(key)).describedAs("key found in the cache").isFalse()
    assertThat(cache.mExclusiveEntries.contains(key))
        .describedAs("key found in the exclusives")
        .isFalse()
  }

  private fun assertOrphanWithCount(entry: CountingMemoryCache.Entry<String, Int>, count: Int) {
    assertThat(entry)
        .describedAs("entry found in the exclusives")
        .isNotSameAs(cache.mCachedEntries[entry.key])
    assertThat(entry)
        .describedAs("entry found in the cache")
        .isNotSameAs(cache.mExclusiveEntries[entry.key])
    assertThat(entry.isOrphan).describedAs("entry is not an orphan").isTrue()
    assertThat(entry.clientCount).describedAs("client count mismatch").isEqualTo(count)
  }

  private fun assertTotalSize(count: Int, bytes: Int) {
    assertThat(cache.count).describedAs("total cache count mismatch").isEqualTo(count)
    assertThat(cache.sizeInBytes).describedAs("total cache size mismatch").isEqualTo(bytes)
  }

  private fun assertExclusivelyOwnedSize(count: Int, bytes: Int) {
    assertThat(cache.evictionQueueCount)
        .describedAs("total exclusives count mismatch")
        .isEqualTo(count)
    assertThat(cache.evictionQueueSizeInBytes)
        .describedAs("total exclusives size mismatch")
        .isEqualTo(bytes)
  }
}
