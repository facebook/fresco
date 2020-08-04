/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.SystemClock;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({SystemClock.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@Config(manifest = Config.NONE)
public class AbstractAdaptiveCountingMemoryCacheTest {

  private static final int CACHE_MAX_SIZE = 1200;
  private static final int CACHE_MAX_COUNT = 4;
  private static final int CACHE_EVICTION_QUEUE_MAX_SIZE = 1100;
  private static final int CACHE_EVICTION_QUEUE_MAX_COUNT = 4;
  private static final int CACHE_ENTRY_MAX_SIZE = 1000;
  private static final long PARAMS_CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

  @Mock public ResourceReleaser<Integer> mReleaser;
  @Mock public AbstractAdaptiveCountingMemoryCache.CacheTrimStrategy mCacheTrimStrategy;
  @Mock public Supplier<MemoryCacheParams> mParamsSupplier;
  @Mock public AbstractAdaptiveCountingMemoryCache.EntryStateObserver<String> mEntryStateObserver;

  @Rule public PowerMockRule rule = new PowerMockRule();

  private MemoryCacheParams mParams;
  AbstractAdaptiveCountingMemoryCache<String, Integer> mCache;
  private ValueDescriptor<Integer> mValueDescriptor;
  private static final String KEY = "KEY";
  private static final String[] KEYS =
      new String[] {"k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9"};
  private static final int initialLFUCacheFractionPromil = 500;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(SystemClock.class);
    PowerMockito.when(SystemClock.uptimeMillis()).thenReturn(0L);
    mValueDescriptor =
        new ValueDescriptor<Integer>() {
          @Override
          public int getSizeInBytes(Integer value) {
            return value;
          }
        };
    mParams =
        new MemoryCacheParams(
            CACHE_MAX_SIZE,
            CACHE_MAX_COUNT,
            CACHE_EVICTION_QUEUE_MAX_SIZE,
            CACHE_EVICTION_QUEUE_MAX_COUNT,
            CACHE_ENTRY_MAX_SIZE,
            PARAMS_CHECK_INTERVAL_MS);
    when(mParamsSupplier.get()).thenReturn(mParams);
  }

  /**
   * test: pass illegal adaptive rate and illegal initial LFU cache fraction. expected result: fall
   * back and use the default adaptive rate and initial LFU fraction values.
   */
  @Test
  public void testPassIllegalArgumentsToTheCacheConstructor() {
    final int illegalAdaptiveRate = -1;
    final int illegalLFUCacheFractionPromil =
        AbstractAdaptiveCountingMemoryCache.MIN_FRACTION_PROMIL - 1;
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            illegalAdaptiveRate,
            1,
            2,
            illegalLFUCacheFractionPromil);
    assertNotNull(mCache);
    assertEquals(
        mCache.mLFUFractionPromil, AbstractAdaptiveCountingMemoryCache.DEFAULT_LFU_FRACTION_PROMIL);
    assertEquals(
        String.valueOf(mCache.mAdaptiveRatePromil),
        String.valueOf(AbstractAdaptiveCountingMemoryCache.DEFAULT_ADAPTIVE_RATE_PROMIL));
  }

  @Test
  public void testCache() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    mCache.cache(KEY, newReference(100));
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 1);
    verify(mReleaser, never()).release(anyInt());
  }

  @Test
  public void testClosingOriginalReference() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> originalRef = newReference(100);
    mCache.cache(KEY, originalRef);
    // cache should make its own copy and closing the original reference after caching
    // should not affect the cached value
    originalRef.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 1);
    verify(mReleaser, never()).release(anyInt());
  }

  @Test
  public void testClosingClientReference() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> cachedRef = mCache.cache(KEY, newReference(100));
    // cached item should get exclusively owned
    cachedRef.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);
    assertExclusivelyOwned(KEY, 100);
    verify(mReleaser, never()).release(anyInt());
  }

  @Test
  public void testNotExclusiveAtFirst() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    mCache.cache(KEY, newReference(100), mEntryStateObserver);
    verify(mEntryStateObserver, never()).onExclusivityChanged((String) any(), anyBoolean());
  }

  @Test
  public void testToggleExclusive() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> cachedRef =
        mCache.cache(KEY, newReference(100), mEntryStateObserver);
    cachedRef.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
    mCache.get(KEY);
    verify(mEntryStateObserver).onExclusivityChanged(KEY, false);
  }

  @Test
  public void testCantReuseNonExclusive() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> cachedRef =
        mCache.cache(KEY, newReference(100), mEntryStateObserver);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    CloseableReference<Integer> reusedRef = mCache.reuse(KEY);
    assertNull(reusedRef);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    verify(mEntryStateObserver, never()).onExclusivityChanged((String) any(), anyBoolean());
    cachedRef.close();
  }

  @Test
  public void testCanReuseExclusive() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> cachedRef =
        mCache.cache(KEY, newReference(100), mEntryStateObserver);
    cachedRef.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);
    cachedRef = mCache.reuse(KEY);
    assertNotNull(cachedRef);
    verify(mEntryStateObserver).onExclusivityChanged(KEY, false);
    assertTotalSize(0, 0);
    assertExclusivelyOwnedSize(0, 0);
    cachedRef.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
  }

  @Test
  public void testReuseExclusive_CacheSameItem() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> cachedRef =
        mCache.cache(KEY, newReference(100), mEntryStateObserver);
    cachedRef.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);
    cachedRef = mCache.reuse(KEY);
    assertNotNull(cachedRef);
    verify(mEntryStateObserver).onExclusivityChanged(KEY, false);
    assertTotalSize(0, 0);
    assertExclusivelyOwnedSize(0, 0);
    CloseableReference<Integer> newItem = mCache.cache(KEY, cachedRef);
    cachedRef.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    newItem.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);
  }

  @Test
  public void testReuseExclusive_CacheSameItemWithDifferentKey() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> cachedRef =
        mCache.cache(KEY, newReference(100), mEntryStateObserver);
    cachedRef.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);
    cachedRef = mCache.reuse(KEY);
    assertNotNull(cachedRef);
    verify(mEntryStateObserver).onExclusivityChanged(KEY, false);
    assertTotalSize(0, 0);
    assertExclusivelyOwnedSize(0, 0);
    CloseableReference<Integer> newItem = mCache.cache(KEYS[2], cachedRef);
    cachedRef.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    newItem.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);
  }

  @Test
  public void testInUseCount() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> cachedRef1 = mCache.cache(KEY, newReference(100));

    CloseableReference<Integer> cachedRef2a = mCache.get(KEY);
    CloseableReference<Integer> cachedRef2b = cachedRef2a.clone();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 2);

    CloseableReference<Integer> cachedRef3a = mCache.get(KEY);
    CloseableReference<Integer> cachedRef3b = cachedRef3a.clone();
    CloseableReference<Integer> cachedRef3c = cachedRef3b.clone();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 3);

    cachedRef1.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 2);

    // all copies of cachedRef2a need to be closed for usage count to drop
    cachedRef2a.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 2);
    cachedRef2b.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 1);

    // all copies of cachedRef3a need to be closed for usage count to drop
    cachedRef3c.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 1);
    cachedRef3b.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 1);
    cachedRef3a.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);
    assertExclusivelyOwned(KEY, 100);
  }

  @Test
  public void testCachingSameKeyTwice() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> originalRef1 = newReference(110);
    CloseableReference<Integer> cachedRef1 = mCache.cache(KEY, originalRef1);
    CloseableReference<Integer> cachedRef2a = mCache.get(KEY);
    CloseableReference<Integer> cachedRef2b = cachedRef2a.clone();
    CloseableReference<Integer> cachedRef3 = mCache.get(KEY);
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry1 =
        mCache.mCachedEntries.get(KEY);

    CloseableReference<Integer> cachedRef2 = mCache.cache(KEY, newReference(120));
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry2 =
        mCache.mCachedEntries.get(KEY);
    assertNotSame(entry1, entry2);
    assertOrphanWithCount(entry1, 3);
    assertSharedWithCount(KEY, 120, 1);

    // release the orphaned reference only when all clients are gone
    originalRef1.close();
    cachedRef2b.close();
    assertOrphanWithCount(entry1, 3);
    cachedRef2a.close();
    assertOrphanWithCount(entry1, 2);
    cachedRef1.close();
    assertOrphanWithCount(entry1, 1);
    verify(mReleaser, never()).release(anyInt());
    cachedRef3.close();
    assertOrphanWithCount(entry1, 0);
    verify(mReleaser).release(110);
  }

  @Test
  public void testDoesNotCacheBigValues() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    assertNull(mCache.cache(KEY, newReference(CACHE_ENTRY_MAX_SIZE + 1)));
  }

  @Test
  public void testDoesCacheNotTooBigValues() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    assertNotNull(mCache.cache(KEY, newReference(CACHE_ENTRY_MAX_SIZE)));
  }

  @Test
  public void testEviction_ByTotalSize() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    // value 4 cannot fit the cache
    CloseableReference<Integer> originalRef1 = newReference(400);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(500);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(100);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    CloseableReference<Integer> originalRef4 = newReference(1000);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    assertTotalSize(3, 1000);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEYS[1], 400, 1);
    assertSharedWithCount(KEYS[2], 500, 1);
    assertSharedWithCount(KEYS[3], 100, 1);
    assertNotCached(KEYS[4], 1000);
    assertNull(valueRef4);

    // closing the clients of cached items will make them viable for eviction
    valueRef1.close();
    valueRef2.close();
    valueRef3.close();
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(1, 100);

    // value 4 can now fit after evicting value1 and value2
    valueRef4 = mCache.cache(KEYS[4], newReference(1000));
    assertTotalSize(2, 1100);
    assertExclusivelyOwnedSize(1, 100);
    assertNotCached(KEYS[1], 400);
    assertNotCached(KEYS[2], 500);
    assertSharedWithCount(KEYS[4], 1000, 1);
    verify(mReleaser).release(400);
    verify(mReleaser).release(500);
    valueRef4.close();
  }

  @Test
  public void testEviction_ByTotalCount() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    // value 5 cannot fit the cache
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    CloseableReference<Integer> originalRef5 = newReference(50);
    CloseableReference<Integer> valueRef5 = mCache.cache(KEYS[5], originalRef5);
    originalRef5.close();
    assertTotalSize(4, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEYS[1], 10, 1);
    assertSharedWithCount(KEYS[2], 20, 1);
    assertSharedWithCount(KEYS[3], 30, 1);
    assertSharedWithCount(KEYS[4], 40, 1);
    assertNotCached(KEYS[5], 50);
    assertNull(valueRef5);

    // closing the clients of cached items will make them viable for eviction
    valueRef1.close();
    valueRef2.close();
    valueRef3.close();
    assertTotalSize(2, 70);
    assertExclusivelyOwnedSize(1, 30);
    assertNotCached(KEYS[1], 10);
    assertNotCached(KEYS[2], 20);

    // value 4 can now fit after evicting value1
    valueRef4 = mCache.cache(KEYS[5], newReference(50));
    assertTotalSize(3, 120);
    assertExclusivelyOwnedSize(1, 30);
    assertNotCached(KEYS[1], 10);
    assertNotCached(KEYS[2], 20);
    assertExclusivelyOwned(KEYS[3], 30);
    assertSharedWithCount(KEYS[4], 40, 1);
    assertSharedWithCount(KEYS[5], 50, 1);
    verify(mReleaser).release(10);
    verify(mReleaser).release(20);
  }

  @Test
  public void testEviction_ByEvictionQueueSize() {
    // Cache has 4 entries; 3 for LFU and 1 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier, mCacheTrimStrategy, mValueDescriptor, 100, 1, 2, 750);

    CloseableReference<Integer> originalRef1 = newReference(200);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(300);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(400);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    assertTotalSize(3, 900);
    assertExclusivelyOwnedSize(2, 500);
    assertExclusivelyOwned(KEYS[1], 200);
    assertExclusivelyOwned(KEYS[2], 300);
    assertSharedWithCount(KEYS[3], 400, 1);

    // closing the client reference for item3 will cause item1 to be evicted
    valueRef3.close();
    assertTotalSize(2, 700);
    assertExclusivelyOwnedSize(2, 700);
    assertNotCached(KEYS[1], 200);
    assertExclusivelyOwned(KEYS[2], 300);
    assertExclusivelyOwned(KEYS[3], 400);
    verify(mReleaser).release(200);
  }

  @Test
  public void testEviction_ByEvictionQueueCount() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3.close();
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    assertTotalSize(2, 70);
    assertExclusivelyOwnedSize(1, 30);
    assertNotCached(KEYS[1], 10);
    assertNotCached(KEYS[2], 20);
    assertExclusivelyOwned(KEYS[3], 30);
    assertSharedWithCount(KEYS[4], 40, 1);

    // closing the client reference for item4 will cause item2 to be evicted as well
    valueRef4.close();
    assertTotalSize(2, 70);
    assertExclusivelyOwnedSize(2, 70);
    assertNotCached(KEYS[1], 10);
    assertNotCached(KEYS[2], 20);
    assertExclusivelyOwned(KEYS[3], 30);
    assertExclusivelyOwned(KEYS[4], 40);
    verify(mReleaser).release(10);
    verify(mReleaser).release(20);
  }

  @Test
  public void testUpdatesCacheParams() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    InOrder inOrder = inOrder(mParamsSupplier);

    CloseableReference<Integer> originalRef = newReference(400);
    CloseableReference<Integer> cachedRef = mCache.cache(KEYS[2], originalRef);
    originalRef.close();
    cachedRef.close();

    mCache.get(KEY);
    inOrder.verify(mParamsSupplier).get();

    PowerMockito.when(SystemClock.uptimeMillis()).thenReturn(PARAMS_CHECK_INTERVAL_MS - 1);
    mCache.get(KEY);
    inOrder.verify(mParamsSupplier, never()).get();
    mCache.get(KEY);
    inOrder.verify(mParamsSupplier, never()).get();

    assertTotalSize(1, 400);
    assertExclusivelyOwnedSize(1, 400);

    mParams =
        new MemoryCacheParams(
            300 /* cache max size */,
            CACHE_MAX_COUNT,
            CACHE_EVICTION_QUEUE_MAX_SIZE,
            CACHE_EVICTION_QUEUE_MAX_COUNT,
            CACHE_ENTRY_MAX_SIZE,
            PARAMS_CHECK_INTERVAL_MS);
    when(mParamsSupplier.get()).thenReturn(mParams);

    PowerMockito.when(SystemClock.uptimeMillis()).thenReturn(PARAMS_CHECK_INTERVAL_MS);
    mCache.get(KEY);
    inOrder.verify(mParamsSupplier).get();

    assertTotalSize(0, 0);
    assertExclusivelyOwnedSize(0, 0);
    verify(mReleaser).release(400);
  }

  @Test
  public void testRemoveAllMatchingPredicate() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item1 to MFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> valueRef1a = mCache.get(KEYS[1]);
    originalRef1.close();
    valueRef1.close();
    valueRef1a.close();

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();

    // insert item3 to the cache
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry3 =
        mCache.mCachedEntries.get(KEYS[3]);

    int numEvictedEntries =
        mCache.removeAll(
            new Predicate<String>() {
              @Override
              public boolean apply(String key) {
                return key.equals(KEYS[2]) || key.equals(KEYS[3]);
              }
            });

    assertEquals(2, numEvictedEntries);
    assertNotCached(KEYS[2], 20);
    assertNotCached(KEYS[3], 30);

    assertTotalSize(1, 10);
    assertExclusivelyOwnedSize(1, 10);
    assertOrphanWithCount(entry3, 1);

    verify(mReleaser).release(20);
    verify(mReleaser, never()).release(30);

    valueRef3.close();
    verify(mReleaser).release(30);
  }

  @Test
  public void testClear() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    CloseableReference<Integer> originalRef1 = newReference(110);
    CloseableReference<Integer> cachedRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry1 =
        mCache.mCachedEntries.get(KEYS[1]);
    CloseableReference<Integer> originalRef2 = newReference(120);
    CloseableReference<Integer> cachedRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    cachedRef2.close();

    mCache.clear();
    assertTotalSize(0, 0);
    assertExclusivelyOwnedSize(0, 0);
    assertOrphanWithCount(entry1, 1);
    assertNotCached(KEYS[2], 120);
    verify(mReleaser).release(120);

    cachedRef1.close();
    verify(mReleaser).release(110);
  }

  @Test
  public void testTrimming() {
    MemoryTrimType memoryTrimType = MemoryTrimType.OnCloseToDalvikHeapLimit;
    mParams = new MemoryCacheParams(2200, 16, 2200, 16, 110, PARAMS_CHECK_INTERVAL_MS);
    when(mParamsSupplier.get()).thenReturn(mParams);
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    PowerMockito.when(SystemClock.uptimeMillis()).thenReturn(PARAMS_CHECK_INTERVAL_MS);
    InOrder inOrder = inOrder(mReleaser);

    // create original references
    CloseableReference<Integer>[] originalRefs = new CloseableReference[10];
    for (int i = 0; i < 10; i++) {
      originalRefs[i] = newReference(100 + i);
    }
    // cache items & close the original references
    CloseableReference<Integer>[] cachedRefs = new CloseableReference[10];
    for (int i = 0; i < 10; i++) {
      cachedRefs[i] = mCache.cache(KEYS[i], originalRefs[i]);
      originalRefs[i].close();
    }
    // cache should keep alive the items until evicted
    inOrder.verify(mReleaser, never()).release(anyInt());

    // trimming cannot evict shared entries
    when(mCacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(1.00);
    mCache.trim(memoryTrimType);
    assertSharedWithCount(KEYS[0], 100, 1);
    assertSharedWithCount(KEYS[1], 101, 1);
    assertSharedWithCount(KEYS[2], 102, 1);
    assertSharedWithCount(KEYS[3], 103, 1);
    assertSharedWithCount(KEYS[4], 104, 1);
    assertSharedWithCount(KEYS[5], 105, 1);
    assertSharedWithCount(KEYS[6], 106, 1);
    assertSharedWithCount(KEYS[7], 107, 1);
    assertSharedWithCount(KEYS[8], 108, 1);
    assertSharedWithCount(KEYS[9], 109, 1);
    assertTotalSize(10, 1045);
    assertExclusivelyOwnedSize(0, 0);

    // close 6 client references
    cachedRefs[4].close();
    cachedRefs[2].close();
    cachedRefs[7].close();
    cachedRefs[3].close();
    cachedRefs[6].close();
    cachedRefs[8].close();
    assertSharedWithCount(KEYS[0], 100, 1);
    assertSharedWithCount(KEYS[1], 101, 1);
    assertSharedWithCount(KEYS[5], 105, 1);
    assertSharedWithCount(KEYS[9], 109, 1);
    assertExclusivelyOwned(KEYS[8], 108);
    assertExclusivelyOwned(KEYS[2], 102);
    assertExclusivelyOwned(KEYS[7], 107);
    assertExclusivelyOwned(KEYS[3], 103);
    assertExclusivelyOwned(KEYS[6], 106);
    assertExclusivelyOwned(KEYS[4], 104);
    assertTotalSize(10, 1045);
    assertExclusivelyOwnedSize(6, 630);

    // Trim cache by 45%. This means that out of total of 1045 bytes cached, 574 should remain.
    // 415 bytes ars used by the clients, which leaves 159 for the exclusively owned items.
    // Only the the most recent exclusively owned item fits, and it occupies 108 bytes.
    when(mCacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(0.45);
    mCache.trim(memoryTrimType);
    assertSharedWithCount(KEYS[0], 100, 1);
    assertSharedWithCount(KEYS[1], 101, 1);
    assertSharedWithCount(KEYS[5], 105, 1);
    assertSharedWithCount(KEYS[9], 109, 1);
    assertExclusivelyOwned(KEYS[8], 108);
    assertNotCached(KEYS[4], 104);
    assertNotCached(KEYS[2], 102);
    assertNotCached(KEYS[7], 107);
    assertNotCached(KEYS[3], 103);
    assertNotCached(KEYS[6], 106);
    assertTotalSize(5, 523);
    assertExclusivelyOwnedSize(1, 108);
    inOrder.verify(mReleaser).release(102);
    inOrder.verify(mReleaser).release(107);
    inOrder.verify(mReleaser).release(103);
    inOrder.verify(mReleaser).release(106);

    cachedRefs[5].close();
    // Full trim. All exclusively owned items should be evicted.
    when(mCacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(1.00);
    mCache.trim(memoryTrimType);
    assertSharedWithCount(KEYS[0], 100, 1);
    assertSharedWithCount(KEYS[1], 101, 1);
    assertSharedWithCount(KEYS[9], 109, 1);
    assertNotCached(KEYS[8], 108);
    assertNotCached(KEYS[2], 102);
    assertNotCached(KEYS[7], 107);
    assertNotCached(KEYS[3], 103);
    assertNotCached(KEYS[6], 106);
    assertNotCached(KEYS[4], 104);
    assertNotCached(KEYS[5], 105);
    assertTotalSize(3, 310);
    assertExclusivelyOwnedSize(0, 0);
    inOrder.verify(mReleaser).release(105);
  }

  @Test
  public void testTrimmingMFU() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    MemoryTrimType memoryTrimType = MemoryTrimType.OnCloseToDalvikHeapLimit;
    mParams = new MemoryCacheParams(2200, 16, 2200, 16, 110, PARAMS_CHECK_INTERVAL_MS);
    when(mParamsSupplier.get()).thenReturn(mParams);
    PowerMockito.when(SystemClock.uptimeMillis()).thenReturn(PARAMS_CHECK_INTERVAL_MS);
    InOrder inOrder = inOrder(mReleaser);

    // insert item1 to MFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> cachedRef1 = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> cachedRef1a = mCache.get(KEYS[1]);
    originalRef1.close();
    cachedRef1.close();
    cachedRef1a.close();

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> cachedRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    cachedRef2.close();

    // Trim 50% of the cache size
    when(mCacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(0.5);
    mCache.trim(memoryTrimType);
    assertExclusivelyOwnedSize(1, 10);
    assertMFUExclusivelyOwned(KEYS[1], 10);

    // insert item3 to LFU
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> cachedRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    cachedRef3.close();

    // insert item2 to MFU
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> cachedRef4 = mCache.cache(KEYS[4], originalRef4);
    CloseableReference<Integer> cachedRef4a = mCache.get(KEYS[4]);
    originalRef4.close();
    cachedRef4.close();
    cachedRef4a.close();

    assertExclusivelyOwnedSize(3, 80);
    // Trim 50% of the cache size, this will remove item3 from LFU and item1 from MFU
    when(mCacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(0.5);
    mCache.trim(memoryTrimType);
    assertNotCached(KEYS[1], 10);
    assertNotCached(KEYS[3], 30);
    assertMFUExclusivelyOwned(KEYS[4], 40);
  }

  @Test
  public void testContains() {
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);
    assertFalse(mCache.contains(KEY));

    CloseableReference<Integer> newRef = mCache.cache(KEY, newReference(100));

    assertTrue(mCache.contains(KEY));
    assertFalse(mCache.contains(KEYS[0]));

    newRef.close();

    assertTrue(mCache.contains(KEY));
    assertFalse(mCache.contains(KEYS[0]));

    CloseableReference<Integer> reuse = mCache.reuse(KEY);
    reuse.close();

    assertFalse(mCache.contains(KEY));
    assertFalse(mCache.contains(KEYS[0]));
  }

  /**
   * Test: insert one item to the cache, access it twice, then close the first reference to the
   * item. Expected: after closing the reference, the ClientCount should be decreased while the
   * accessCount should not change.
   */
  @Test
  public void testAccessCountUpdate() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // add MFU item1
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> valueRef1a = mCache.get(KEYS[1]);
    originalRef1.close();
    assertSharedWithCount(KEYS[1], 10, 2);
    assertSharedWithAccessCount(KEYS[1], 10, 2);

    // close the first reference
    valueRef1.close();
    assertSharedWithCount(KEYS[1], 10, 1);
    assertSharedWithAccessCount(KEYS[1], 10, 2);
    valueRef1a.close();
  }

  /**
   * Test: Add 2 frequently used elements, add a new frequently used element and make sure the cache
   * evicts the 1st item inserted to the cache (the least recently used one).
   */
  @Test
  public void testFrequentlyUsedEviction() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // add item1 to MFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> valueRef1a = mCache.get(KEYS[1]);
    originalRef1.close();
    valueRef1.close();
    valueRef1a.close();

    // add item2 to MFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    CloseableReference<Integer> valueRef2a = mCache.get(KEYS[2]);
    originalRef2.close();
    valueRef2.close();
    assertSharedWithCount(KEYS[2], 20, 1);
    assertSharedWithAccessCount(KEYS[2], 20, 2);
    valueRef2a.close();

    // both items are in the MFU
    assertExclusivelyOwned(KEYS[1], 10);
    assertExclusivelyOwned(KEYS[2], 20);

    // add item2 to MFU, as a result item1 should be evicted
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    CloseableReference<Integer> valueRef3a = mCache.get(KEYS[3]);
    originalRef3.close();
    valueRef3.close();
    valueRef3a.close();
    assertExclusivelyOwned(KEYS[2], 20);
    assertExclusivelyOwned(KEYS[3], 30);
  }

  /**
   * add 2 frequently used items and one LFU item. Then add a new frequently used item. Expected:
   * the cache should evict one of the MFU items and not the LFU item.
   */
  @Test
  public void testFrequentlyUsedEvictionWhithoutChangingLFU() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item1 to MFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1a = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> valueRef1b = mCache.get(KEYS[1]);
    originalRef1.close();
    valueRef1a.close();
    valueRef1b.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);

    // insert item3 to MFU
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3a = mCache.cache(KEYS[3], originalRef3);
    CloseableReference<Integer> valueRef3b = mCache.get(KEYS[3]);
    originalRef3.close();
    valueRef3a.close();
    valueRef3b.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);
    assertMFUExclusivelyOwned(KEYS[3], 30);
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertExclusivelyOwnedSize(3, 60);

    // insert item4 to MFU, this should evict item1 from the cache
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4a = mCache.cache(KEYS[4], originalRef4);
    CloseableReference<Integer> valueRef4b = mCache.get(KEYS[4]);
    originalRef4.close();
    valueRef4a.close();
    valueRef4b.close();
    assertNotCached(KEYS[1], 10);
    assertMFUExclusivelyOwned(KEYS[3], 30);
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertMFUExclusivelyOwned(KEYS[4], 40);
  }

  /**
   * insert 2 LFU keys, and 1 MFU key. Make sure when adding a new LFU key, the cache evicts the LFU
   * LFU key and not the LRU MFU key.
   */
  @Test
  public void testEvictLFUandNotMFUKeys() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item1 to MFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1a = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> valueRef1b = mCache.get(KEYS[1]);
    originalRef1.close();
    valueRef1a.close();
    valueRef1b.close(); // item1 is in the MFU cache
    assertMFUExclusivelyOwned(KEYS[1], 10);

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2a = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2a.close();

    // insert item3 to LFU
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3a = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3a.close();

    assertMFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertLFUExclusivelyOwned(KEYS[3], 30);

    // insert item4 to LFU, the cache should evict item2
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4a = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    valueRef4a.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[3], 30);
    assertLFUExclusivelyOwned(KEYS[4], 40);
  }

  /**
   * insert 1 MFU item and 1 LFU item, access the LFU item twice (to become MFU) and make sure it is
   * moved to the MFU cache.
   */
  @Test
  public void testMoveItemsFromLFUToMFU() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item2 to MFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1a = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> valueRef1b = mCache.get(KEYS[1]);
    originalRef1.close();
    valueRef1a.close();
    valueRef1b.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);

    // access item2 one more time, this should result in moving item2 from LFU to MFU cache.
    CloseableReference<Integer> valueRef2a = mCache.get(KEYS[2]);
    valueRef2a.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);
    assertMFUExclusivelyOwned(KEYS[2], 20);
  }

  /**
   * This test makes sure that the accessCount state is updated for keys in the ghost list. Test:
   * add item1 to the ghost list, and try to access this element (after it was evicted), and check
   * that the access number was updated in the ghost list.
   */
  @Test
  public void testAccessCountUpdateInLFUGhostList() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item1 to LFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1a = mCache.cache(KEYS[1], originalRef1);
    valueRef1a.close();

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2a = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2a.close();

    // insert item3 to LFU, this should evict item1 from LFU and insert it to the LFU ghost list
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3a = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3a.close();
    assertExclusivelyOwnedSize(2, 50);
    assertKeyIsInLFUGhostList(KEYS[1], 1);

    // try to access the evicted item: item1, and check its accessCount is updated correctly
    CloseableReference<Integer> valueRef1b = mCache.get(KEYS[1]);
    assertNull(valueRef1b);
    assertExclusivelyOwnedSize(2, 50);
    assertKeyIsInLFUGhostList(KEYS[1], 2);

    // validate item1's state is correct after inserting the element again to the cache.
    CloseableReference<Integer> valueRef1c = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    assertSharedWithAccessCount(KEYS[1], 10, 3);
    // also, make sure it was added to the MFU now.
    valueRef1c.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);
  }

  /** check if the MFU keys are inserted to the MFU ghost list after eviction. */
  @Test
  public void testAddKeyToMFHGhostList() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item1 to MFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1a = mCache.cache(KEYS[1], originalRef1);
    CloseableReference<Integer> valueRef1b = mCache.get(KEYS[1]);
    originalRef1.close();
    valueRef1a.close();
    valueRef1b.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);

    // insert item2 to MFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2a = mCache.cache(KEYS[2], originalRef2);
    CloseableReference<Integer> valueRef2b = mCache.get(KEYS[2]);
    originalRef2.close();
    valueRef2a.close();
    valueRef2b.close();
    assertMFUExclusivelyOwned(KEYS[1], 10);
    assertMFUExclusivelyOwned(KEYS[2], 20);

    // insert item3 to MFU, the cache should evict item1 and insert it to the MFU ghost list
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3a = mCache.cache(KEYS[3], originalRef3);
    CloseableReference<Integer> valueRef3b = mCache.get(KEYS[3]);
    originalRef3.close();
    valueRef3a.close();
    valueRef3b.close();
    assertMFUExclusivelyOwned(KEYS[2], 20);
    assertMFUExclusivelyOwned(KEYS[3], 30);
    assertKeyIsInMFUGhostList(KEYS[1]);
  }

  /** checks that MFU min size cannot get smaller than MIN_MFU_FRACTION_PROMIL. */
  @Test
  public void testCacheMargins() {
    // Cache has 4 entries; 3 for LFU and 1 for MFU
    // key is considered as MFU, if its accessCount > 1
    int initialLFUFractionPromil = 850;
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUFractionPromil);

    // insert item1 to LFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1a = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1a.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2a = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2a.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);

    // insert item3 to LFU
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3a = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3a.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertLFUExclusivelyOwned(KEYS[3], 30);

    // insert item4 to LFU, this will evict item1
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4a = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    valueRef4a.close();
    assertNotCached(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertLFUExclusivelyOwned(KEYS[3], 30);
    assertLFUExclusivelyOwned(KEYS[4], 40);

    // item1 was evicted, now in ghost list
    // this get should have increased the LFU fraction but since the MFU fraction will get under
    // MIN_MFU_FRACTION_PREOMIL, the fractions will not be updated
    CloseableReference<Integer> valueRef1b = mCache.get(KEYS[1]);
    assertNull(valueRef1b);
    assertEquals(initialLFUFractionPromil, mCache.mLFUFractionPromil);
  }

  /**
   * This test checks that the ghost list is actually an LRU list. It checks if the position of the
   * keys in the LFU ghost list is updated when trying to access them after being evicted (but they
   * were found it in the ghost list). ExpectedL the accessed key will be moved to the youngest
   * position in the ghost list.
   */
  @Test
  public void testPositionUpdateInGhostList() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item1 to LFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1a = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1a.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);
    assertGhostListsValidSize();

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2a = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2a.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertGhostListsValidSize();

    // insert item3 to ghost list, this will result in evicting item1 from the LFU and insert it to
    // the LFU ghost list.
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3a = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3a.close();
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertLFUExclusivelyOwned(KEYS[3], 30);
    assertKeyIsInLFUGhostList(KEYS[1], 1);
    assertGhostListsValidSize();

    // insert item4 to LFU, this will result in evicting item2 from the LFU and inset it to the LFU
    // ghost list.
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4a = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    valueRef4a.close();
    assertLFUExclusivelyOwned(KEYS[3], 30);
    assertLFUExclusivelyOwned(KEYS[4], 40);
    assertKeyIsInLFUGhostList(KEYS[1], 1);
    assertKeyIsInLFUGhostList(KEYS[2], 1);
    assertGhostListsValidSize();

    // try to access item1 (which is already evicted).
    CloseableReference<Integer> valueRef1b = mCache.get(KEYS[1]);
    assertNull(valueRef1b);
    assertKeyIsInLFUGhostList(KEYS[1], 2);
    assertGhostListsValidSize();

    // insert item5 to LFU, this will result in evicting item3 from LFU and insert it to the LFU
    // ghost list
    CloseableReference<Integer> originalRef5 = newReference(50);
    CloseableReference<Integer> valueRef5a = mCache.cache(KEYS[5], originalRef5);
    originalRef5.close();
    valueRef5a.close();

    // make sure item2 is removed from LFU ghost list and not item1 (since they have replaced
    // their positions in the ghost list)
    assertGhostListsValidSize();
    assertLFUExclusivelyOwned(KEYS[4], 40);
    assertLFUExclusivelyOwned(KEYS[5], 50);
    assertKeyIsInLFUGhostList(KEYS[1], 2);
    assertKeyIsInLFUGhostList(KEYS[3], 1);
    assertKeyIsNotInLFUGhostList(KEYS[2]);
  }

  /**
   * This test maskes sure that the LFU cache fraction is being updated correctly when trying to
   * access a recently evicted key from LFU. Test: add 3 LFU items, the 1st item will be evicted and
   * inserted to the LFU ghost list (the LFU size is up to 2 keys). try to access the recently
   * evicted key and make sure that the LFU cache fraction is increased by the value of the
   * adaptiveRate.
   */
  @Test
  public void testFractionUpdate() {
    // Cache has 4 entries; 2 for LFU and 2 for MFU
    // key is considered as MFU, if its accessCount > 1
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            100,
            1,
            2,
            initialLFUCacheFractionPromil);

    // insert item1 to LFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);

    // insert item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);

    // insert item3 to LFU, item1 will be evicted from LFU and inserted to the LFU ghost list
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3.close();
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertLFUExclusivelyOwned(KEYS[3], 30);
    assertNotCached(KEYS[1], 10);
    assertKeyIsInLFUGhostList(KEYS[1], 1);
    assertEquals(mCache.mLFUFractionPromil, initialLFUCacheFractionPromil);

    // access item1, which was already evicted from LFU
    CloseableReference<Integer> valueRef1a = mCache.get(KEYS[1]);
    assertNull(valueRef1a);
    // checks if the LFU fraction has been increased by adativeRate value
    assertEquals(
        initialLFUCacheFractionPromil + mCache.mAdaptiveRatePromil, mCache.mLFUFractionPromil);

    // make item2 "frequently used" by accessing it more than mFrequentlyUsedThreshiold
    // notice, MFU max size is 1 now (LFU size is 3).
    CloseableReference<Integer> valueRef2a = mCache.get(KEYS[2]);
    valueRef2a.close();
    assertMFUExclusivelyOwned(KEYS[2], 20);

    // insert item4 to MFU, item2 will be evicted from MFU and inserted in the MFU ghost list
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    CloseableReference<Integer> valueRef4a = mCache.get(KEYS[4]);
    originalRef4.close();
    valueRef4.close();
    valueRef4a.close();
    assertEquals(
        initialLFUCacheFractionPromil + mCache.mAdaptiveRatePromil, mCache.mLFUFractionPromil);
  }

  /**
   * This test checks edge cases, when trying to increase/decrease the LFU cache fraction to more
   * than 1/less than 0 (of the cache size). The expected behaviour is not to update the LFU
   * fraction.
   */
  @Test
  public void testLFUFractionOverflow() {
    final int adaptiveRate = 700; // in order to cause an overflow
    mCache =
        createDummyAdaptiveCountingMemoryCache(
            mParamsSupplier,
            mCacheTrimStrategy,
            mValueDescriptor,
            adaptiveRate,
            1,
            2,
            initialLFUCacheFractionPromil);

    // add item1 to LFU
    CloseableReference<Integer> originalRef1 = newReference(10);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);

    // add item2 to LFU
    CloseableReference<Integer> originalRef2 = newReference(20);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    assertLFUExclusivelyOwned(KEYS[1], 10);
    assertLFUExclusivelyOwned(KEYS[2], 20);

    // add item3 to LFU, item1 will be evicted from LFU and will be inserted to the LFU ghost list
    CloseableReference<Integer> originalRef3 = newReference(30);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3.close();
    assertLFUExclusivelyOwned(KEYS[2], 20);
    assertLFUExclusivelyOwned(KEYS[3], 30);
    assertNotCached(KEYS[1], 10);

    // item1 in the LFU ghost list
    assertKeyIsInLFUGhostList(KEYS[1], 1);

    // try to access item1, which was already evicted, which will result in updating the LFU
    // fraction
    assertEquals(initialLFUCacheFractionPromil, mCache.mLFUFractionPromil);
    CloseableReference<Integer> valueRef1a = mCache.get(KEYS[1]);
    assertNull(valueRef1a);
    // make sure the LFU cache fraction was not updated, because if we do it will cause in overflow
    assertEquals(initialLFUCacheFractionPromil, mCache.mLFUFractionPromil);

    // make item2 and item3 "frequently used" by accessing them more than mFrequentlyUsedThreshiold
    CloseableReference<Integer> valueRef2a = mCache.get(KEYS[2]);
    CloseableReference<Integer> valueRef3a = mCache.get(KEYS[3]);
    valueRef2a.close();
    valueRef3a.close();
    assertMFUExclusivelyOwned(KEYS[2], 20);
    assertMFUExclusivelyOwned(KEYS[3], 30);

    // insert item4 to MFU, item2 will be evicted from MFU and inserted in the MFU ghost list
    CloseableReference<Integer> originalRef4 = newReference(40);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    CloseableReference<Integer> valueRef4a = mCache.get(KEYS[4]);
    originalRef4.close();
    valueRef4.close();
    valueRef4a.close();

    assertEquals(initialLFUCacheFractionPromil, mCache.mLFUFractionPromil);
    CloseableReference<Integer> valueRef2b = mCache.get(KEYS[2]);
    assertNull(valueRef2b);
    // make sure the LFU cache fraction was not updated, because if we do it will cause in overflow
    assertEquals(initialLFUCacheFractionPromil, mCache.mLFUFractionPromil);
  }

  private CloseableReference<Integer> newReference(int size) {
    return CloseableReference.of(size, mReleaser);
  }

  private void assertSharedWithCount(String key, Integer value, int count) {
    assertTrue("key not found in the cache", mCache.mCachedEntries.contains(key));
    assertFalse(
        "key found in the LFU exclusives",
        mCache.mLeastFrequentlyUsedExclusiveEntries.contains(key));
    assertFalse(
        "key found in the MFU exclusives",
        mCache.mMostFrequentlyUsedExclusiveEntries.contains(key));
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry =
        mCache.mCachedEntries.get(key);
    assertNotNull("entry not found in the cache", entry);
    assertEquals("key mismatch", key, entry.key);
    assertEquals("value mismatch", value, entry.valueRef.get());
    assertEquals("client count mismatch", count, entry.clientCount);
    assertFalse("entry is an orphan", entry.isOrphan);
  }

  private void assertSharedWithAccessCount(String key, Integer value, int count) {
    assertTrue("key not found in the cache", mCache.mCachedEntries.contains(key));
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry =
        mCache.mCachedEntries.get(key);
    assertNotNull("entry not found in the cache", entry);
    assertEquals("key mismatch", key, entry.key);
    assertEquals("value mismatch", value, entry.valueRef.get());
    assertEquals("access count mismatch", count, entry.accessCount);
    assertFalse("entry is an orphan", entry.isOrphan);
  }

  private void assertExclusivelyOwned(String key, Integer value) {
    assertTrue("key not found in the cache", mCache.mCachedEntries.contains(key));
    assertTrue(
        "key not found in the exclusives",
        mCache.mLeastFrequentlyUsedExclusiveEntries.contains(key)
            || mCache.mMostFrequentlyUsedExclusiveEntries.contains(key));
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry =
        mCache.mCachedEntries.get(key);
    assertNotNull("entry not found in the cache", entry);
    assertEquals("key mismatch", key, entry.key);
    assertEquals("value mismatch", value, entry.valueRef.get());
    assertEquals("client count greater than zero", 0, entry.clientCount);
    assertFalse("entry is an orphan", entry.isOrphan);
  }

  private void assertMFUExclusivelyOwned(String key, Integer value) {
    assertTrue("key not found in the cache", mCache.mCachedEntries.contains(key));
    assertTrue(
        "key not found in the exclusives",
        mCache.mMostFrequentlyUsedExclusiveEntries.contains(key));
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry =
        mCache.mCachedEntries.get(key);
    assertNotNull("entry not found in the cache", entry);
    assertEquals("key mismatch", key, entry.key);
    assertEquals("value mismatch", value, entry.valueRef.get());
    assertEquals("client count greater than zero", 0, entry.clientCount);
    assertFalse("entry is an orphan", entry.isOrphan);
  }

  private void assertKeyIsInLFUGhostList(String key, int accessCount) {
    assertTrue(
        "Key not found in LFU Ghost List", mCache.mLeastFrequentlyUsedKeysGhostList.contains(key));
    assertFalse("Key found in cache", mCache.mCachedEntries.contains(key));
    assertFalse(
        "Key found in exclusive",
        mCache.mLeastFrequentlyUsedExclusiveEntries.contains(key)
            || mCache.mMostFrequentlyUsedExclusiveEntries.contains(key));
    assertTrue(
        "Key not found in LFU ghost list", mCache.mLeastFrequentlyUsedKeysGhostList.contains(key));
    int keyAccessCount = mCache.mLeastFrequentlyUsedKeysGhostList.getValue(key);
    assertNotNull(keyAccessCount);
    assertEquals("Mismatch in access count in ghost list", accessCount, keyAccessCount);
  }

  private void assertKeyIsNotInLFUGhostList(String key) {
    assertFalse(
        "Key found in LFU Ghost List", mCache.mLeastFrequentlyUsedKeysGhostList.contains(key));
  }

  private void assertGhostListsValidSize() {
    assertTrue(
        "LFU keys ghost list overflows",
        mCache.mLeastFrequentlyUsedKeysGhostList.size() <= mCache.mGhostListMaxSize);
    assertTrue(
        "MFU keys ghost list overflows",
        mCache.mMostFrequentlyUsedKeysGhostList.size() <= mCache.mGhostListMaxSize);
  }

  private void assertKeyIsInMFUGhostList(String key) {
    assertFalse(
        "Key found in LFU Ghost List", mCache.mLeastFrequentlyUsedKeysGhostList.contains(key));
    assertFalse("Key found in cache", mCache.mCachedEntries.contains(key));
    assertFalse(
        "Key found in exclusive",
        mCache.mLeastFrequentlyUsedExclusiveEntries.contains(key)
            || mCache.mMostFrequentlyUsedExclusiveEntries.contains(key));
    assertTrue(
        "Key not found in MFU ghost list", mCache.mMostFrequentlyUsedKeysGhostList.contains(key));
  }

  private void assertLFUExclusivelyOwned(String key, Integer value) {
    assertTrue("key not found in the cache", mCache.mCachedEntries.contains(key));
    assertTrue(
        "key not found in the exclusives",
        mCache.mLeastFrequentlyUsedExclusiveEntries.contains(key));
    AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry =
        mCache.mCachedEntries.get(key);
    assertNotNull("entry not found in the cache", entry);
    assertEquals("key mismatch", key, entry.key);
    assertEquals("value mismatch", value, entry.valueRef.get());
    assertEquals("client count greater than zero", 0, entry.clientCount);
    assertFalse("entry is an orphan", entry.isOrphan);
  }

  private void assertNotCached(String key, Integer value) {
    assertFalse("key found in the cache", mCache.mCachedEntries.contains(key));
    assertFalse(
        "key found in the exclusives",
        mCache.mLeastFrequentlyUsedExclusiveEntries.contains(key)
            || mCache.mMostFrequentlyUsedExclusiveEntries.contains(key));
  }

  private void assertOrphanWithCount(
      AbstractAdaptiveCountingMemoryCache.Entry<String, Integer> entry, int count) {
    assertNotSame("entry found in the cahce", entry, mCache.mCachedEntries.get(entry.key));
    assertNotSame(
        "entry found in the LFU execlusive",
        entry,
        mCache.mLeastFrequentlyUsedExclusiveEntries.get(entry.key));
    assertNotSame(
        "entry found in the MFU execlusive",
        entry,
        mCache.mMostFrequentlyUsedExclusiveEntries.get(entry.key));
    assertTrue("entry is not an orphan", entry.isOrphan);
    assertEquals("client count mismatch", count, entry.clientCount);
  }

  private void assertTotalSize(int count, int bytes) {
    assertEquals("total cache count mismatch", count, mCache.getCount());
    assertEquals("total cache size mismatch", bytes, mCache.getSizeInBytes());
  }

  private void assertExclusivelyOwnedSize(int count, int bytes) {
    assertEquals("total exclusives count mismatch", count, mCache.getEvictionQueueCount());
    assertEquals("total exclusives size mismatch", bytes, mCache.getEvictionQueueSizeInBytes());
  }

  private AbstractAdaptiveCountingMemoryCache<String, Integer>
      createDummyAdaptiveCountingMemoryCache(
          Supplier<MemoryCacheParams> memoryCacheParamsSupplier,
          AbstractAdaptiveCountingMemoryCache.CacheTrimStrategy cacheTrimStrategy,
          ValueDescriptor<Integer> valueDescriptor,
          int adaptiveRatePromil,
          int frequentlyUsedThreshold,
          int ghostListMaxSize,
          int lfuFractionPromil) {
    return new AbstractAdaptiveCountingMemoryCache<String, Integer>(
        memoryCacheParamsSupplier,
        cacheTrimStrategy,
        valueDescriptor,
        adaptiveRatePromil,
        frequentlyUsedThreshold,
        ghostListMaxSize,
        lfuFractionPromil) {
      @Override
      protected void logIllegalLfuFraction() {}

      @Override
      protected void logIllegalAdaptiveRate() {}

      @Nullable
      @Override
      public String getDebugData() {
        return null;
      }
    };
  }
}
