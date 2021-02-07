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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.SystemClock;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import java.util.concurrent.TimeUnit;
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
public class LruCountingMemoryCacheTest {

  private static final int CACHE_MAX_SIZE = 1200;
  private static final int CACHE_MAX_COUNT = 4;
  private static final int CACHE_EVICTION_QUEUE_MAX_SIZE = 1100;
  private static final int CACHE_EVICTION_QUEUE_MAX_COUNT = 3;
  private static final int CACHE_ENTRY_MAX_SIZE = 1000;
  private static final long PARAMS_CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

  @Mock public ResourceReleaser<Integer> mReleaser;
  @Mock public MemoryCache.CacheTrimStrategy mCacheTrimStrategy;
  @Mock public Supplier<MemoryCacheParams> mParamsSupplier;
  @Mock public CountingMemoryCache.EntryStateObserver<String> mEntryStateObserver;
  @Mock public Bitmap mBitmap;

  @Rule public PowerMockRule rule = new PowerMockRule();

  private ValueDescriptor<Integer> mValueDescriptor;
  private MemoryCacheParams mParams;
  private LruCountingMemoryCache<String, Integer> mCache;
  private CloseableReference<Bitmap> mBitmapReference;

  private static final String KEY = "KEY";
  private static final String[] KEYS =
      new String[] {"k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9"};

  private static final ResourceReleaser<Bitmap> FAKE_BITMAP_RESOURCE_RELEASER =
      new ResourceReleaser<Bitmap>() {

        @Override
        public void release(Bitmap value) {}
      };

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
    mBitmapReference = CloseableReference.of(mBitmap, FAKE_BITMAP_RESOURCE_RELEASER);
    mCache =
        new LruCountingMemoryCache<>(mValueDescriptor, mCacheTrimStrategy, mParamsSupplier, null);
  }

  @Test
  public void testCache() {
    mCache.cache(KEY, newReference(100));
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEY, 100, 1);
    verify(mReleaser, never()).release(anyInt());
  }

  @Test
  public void testClosingOriginalReference() {
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
    mCache.cache(KEY, newReference(100), mEntryStateObserver);
    verify(mEntryStateObserver, never()).onExclusivityChanged(anyString(), anyBoolean());
  }

  @Test
  public void testToggleExclusive() {
    CloseableReference<Integer> cachedRef =
        mCache.cache(KEY, newReference(100), mEntryStateObserver);
    cachedRef.close();
    verify(mEntryStateObserver).onExclusivityChanged(KEY, true);
    mCache.get(KEY);
    verify(mEntryStateObserver).onExclusivityChanged(KEY, false);
  }

  @Test
  public void testCantReuseNonExclusive() {
    CloseableReference<Integer> cachedRef =
        mCache.cache(KEY, newReference(100), mEntryStateObserver);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    CloseableReference<Integer> reusedRef = mCache.reuse(KEY);
    assertNull(reusedRef);
    assertTotalSize(1, 100);
    assertExclusivelyOwnedSize(0, 0);
    verify(mEntryStateObserver, never()).onExclusivityChanged(anyString(), anyBoolean());
    cachedRef.close();
  }

  @Test
  public void testCanReuseExclusive() {
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
    CloseableReference<Integer> originalRef1 = newReference(110);
    CloseableReference<Integer> cachedRef1 = mCache.cache(KEY, originalRef1);
    CloseableReference<Integer> cachedRef2a = mCache.get(KEY);
    CloseableReference<Integer> cachedRef2b = cachedRef2a.clone();
    CloseableReference<Integer> cachedRef3 = mCache.get(KEY);
    CountingMemoryCache.Entry<String, Integer> entry1 = mCache.mCachedEntries.get(KEY);

    CloseableReference<Integer> cachedRef2 = mCache.cache(KEY, newReference(120));
    CountingMemoryCache.Entry<String, Integer> entry2 = mCache.mCachedEntries.get(KEY);
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
    assertNull(mCache.cache(KEY, newReference(CACHE_ENTRY_MAX_SIZE + 1)));
  }

  @Test
  public void testDoesCacheNotTooBigValues() {
    assertNotNull(mCache.cache(KEY, newReference(CACHE_ENTRY_MAX_SIZE)));
  }

  @Test
  public void testEviction_ByTotalSize() {
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
    CloseableReference<Integer> originalRef4 = newReference(700);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    assertTotalSize(3, 1000);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEYS[1], 400, 1);
    assertSharedWithCount(KEYS[2], 500, 1);
    assertSharedWithCount(KEYS[3], 100, 1);
    assertNotCached(KEYS[4], 700);
    assertNull(valueRef4);

    // closing the clients of cached items will make them viable for eviction
    valueRef1.close();
    valueRef2.close();
    valueRef3.close();
    assertTotalSize(3, 1000);
    assertExclusivelyOwnedSize(3, 1000);

    // value 4 can now fit after evicting value1 and value2
    valueRef4 = mCache.cache(KEYS[4], newReference(700));
    assertTotalSize(2, 800);
    assertExclusivelyOwnedSize(1, 100);
    assertNotCached(KEYS[1], 400);
    assertNotCached(KEYS[2], 500);
    assertExclusivelyOwned(KEYS[3], 100);
    assertSharedWithCount(KEYS[4], 700, 1);
    verify(mReleaser).release(400);
    verify(mReleaser).release(500);
  }

  @Test
  public void testEviction_ByTotalCount() {
    // value 5 cannot fit the cache
    CloseableReference<Integer> originalRef1 = newReference(110);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(120);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(130);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    CloseableReference<Integer> originalRef4 = newReference(140);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    CloseableReference<Integer> originalRef5 = newReference(150);
    CloseableReference<Integer> valueRef5 = mCache.cache(KEYS[5], originalRef5);
    originalRef5.close();
    assertTotalSize(4, 500);
    assertExclusivelyOwnedSize(0, 0);
    assertSharedWithCount(KEYS[1], 110, 1);
    assertSharedWithCount(KEYS[2], 120, 1);
    assertSharedWithCount(KEYS[3], 130, 1);
    assertSharedWithCount(KEYS[4], 140, 1);
    assertNotCached(KEYS[5], 150);
    assertNull(valueRef5);

    // closing the clients of cached items will make them viable for eviction
    valueRef1.close();
    valueRef2.close();
    valueRef3.close();
    assertTotalSize(4, 500);
    assertExclusivelyOwnedSize(3, 360);

    // value 4 can now fit after evicting value1
    valueRef4 = mCache.cache(KEYS[5], newReference(150));
    assertTotalSize(4, 540);
    assertExclusivelyOwnedSize(2, 250);
    assertNotCached(KEYS[1], 110);
    assertExclusivelyOwned(KEYS[2], 120);
    assertExclusivelyOwned(KEYS[3], 130);
    assertSharedWithCount(KEYS[4], 140, 1);
    assertSharedWithCount(KEYS[5], 150, 1);
    verify(mReleaser).release(110);
  }

  @Test
  public void testEviction_ByEvictionQueueSize() {
    CloseableReference<Integer> originalRef1 = newReference(200);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(300);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(700);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    assertTotalSize(3, 1200);
    assertExclusivelyOwnedSize(2, 500);
    assertExclusivelyOwned(KEYS[1], 200);
    assertExclusivelyOwned(KEYS[2], 300);
    assertSharedWithCount(KEYS[3], 700, 1);
    verify(mReleaser, never()).release(anyInt());

    // closing the client reference for item3 will cause item1 to be evicted
    valueRef3.close();
    assertTotalSize(2, 1000);
    assertExclusivelyOwnedSize(2, 1000);
    assertNotCached(KEYS[1], 200);
    assertExclusivelyOwned(KEYS[2], 300);
    assertExclusivelyOwned(KEYS[3], 700);
    verify(mReleaser).release(200);
  }

  @Test
  public void testEviction_ByEvictionQueueCount() {
    CloseableReference<Integer> originalRef1 = newReference(110);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(120);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(130);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    valueRef3.close();
    CloseableReference<Integer> originalRef4 = newReference(140);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();
    assertTotalSize(4, 500);
    assertExclusivelyOwnedSize(3, 360);
    assertExclusivelyOwned(KEYS[1], 110);
    assertExclusivelyOwned(KEYS[2], 120);
    assertExclusivelyOwned(KEYS[3], 130);
    assertSharedWithCount(KEYS[4], 140, 1);
    verify(mReleaser, never()).release(anyInt());

    // closing the client reference for item4 will cause item1 to be evicted
    valueRef4.close();
    assertTotalSize(3, 390);
    assertExclusivelyOwnedSize(3, 390);
    assertNotCached(KEYS[1], 110);
    assertExclusivelyOwned(KEYS[2], 120);
    assertExclusivelyOwned(KEYS[3], 130);
    assertExclusivelyOwned(KEYS[4], 140);
    verify(mReleaser).release(110);
  }

  @Test
  public void testUpdatesCacheParams() {
    InOrder inOrder = inOrder(mParamsSupplier);

    CloseableReference<Integer> originalRef = newReference(700);
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

    assertTotalSize(1, 700);
    assertExclusivelyOwnedSize(1, 700);

    mParams =
        new MemoryCacheParams(
            500 /* cache max size */,
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
    verify(mReleaser).release(700);
  }

  @Test
  public void testRemoveAllMatchingPredicate() {
    CloseableReference<Integer> originalRef1 = newReference(110);
    CloseableReference<Integer> valueRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    valueRef1.close();
    CloseableReference<Integer> originalRef2 = newReference(120);
    CloseableReference<Integer> valueRef2 = mCache.cache(KEYS[2], originalRef2);
    originalRef2.close();
    valueRef2.close();
    CloseableReference<Integer> originalRef3 = newReference(130);
    CloseableReference<Integer> valueRef3 = mCache.cache(KEYS[3], originalRef3);
    originalRef3.close();
    CountingMemoryCache.Entry<String, Integer> entry3 = mCache.mCachedEntries.get(KEYS[3]);
    CloseableReference<Integer> originalRef4 = newReference(150);
    CloseableReference<Integer> valueRef4 = mCache.cache(KEYS[4], originalRef4);
    originalRef4.close();

    int numEvictedEntries =
        mCache.removeAll(
            new Predicate<String>() {
              @Override
              public boolean apply(String key) {
                return key.equals(KEYS[2]) || key.equals(KEYS[3]);
              }
            });

    assertEquals(2, numEvictedEntries);

    assertTotalSize(2, 260);
    assertExclusivelyOwnedSize(1, 110);
    assertExclusivelyOwned(KEYS[1], 110);
    assertNotCached(KEYS[2], 120);
    assertOrphanWithCount(entry3, 1);
    assertSharedWithCount(KEYS[4], 150, 1);

    verify(mReleaser).release(120);
    verify(mReleaser, never()).release(130);

    valueRef3.close();
    verify(mReleaser).release(130);
  }

  @Test
  public void testClear() {
    CloseableReference<Integer> originalRef1 = newReference(110);
    CloseableReference<Integer> cachedRef1 = mCache.cache(KEYS[1], originalRef1);
    originalRef1.close();
    CountingMemoryCache.Entry<String, Integer> entry1 = mCache.mCachedEntries.get(KEYS[1]);
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
    mParams = new MemoryCacheParams(1100, 10, 1100, 10, 110, PARAMS_CHECK_INTERVAL_MS);
    when(mParamsSupplier.get()).thenReturn(mParams);
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

    // close 7 client references
    cachedRefs[8].close();
    cachedRefs[2].close();
    cachedRefs[7].close();
    cachedRefs[3].close();
    cachedRefs[6].close();
    cachedRefs[4].close();
    cachedRefs[5].close();
    assertSharedWithCount(KEYS[0], 100, 1);
    assertSharedWithCount(KEYS[1], 101, 1);
    assertSharedWithCount(KEYS[9], 109, 1);
    assertExclusivelyOwned(KEYS[8], 108);
    assertExclusivelyOwned(KEYS[2], 102);
    assertExclusivelyOwned(KEYS[7], 107);
    assertExclusivelyOwned(KEYS[3], 103);
    assertExclusivelyOwned(KEYS[6], 106);
    assertExclusivelyOwned(KEYS[4], 104);
    assertExclusivelyOwned(KEYS[5], 105);
    assertTotalSize(10, 1045);
    assertExclusivelyOwnedSize(7, 735);

    // Trim cache by 45%. This means that out of total of 1045 bytes cached, 574 should remain.
    // 310 bytes is used by the clients, which leaves 264 for the exclusively owned items.
    // Only the two most recent exclusively owned items fit, and they occupy 209 bytes.
    when(mCacheTrimStrategy.getTrimRatio(memoryTrimType)).thenReturn(0.45);
    mCache.trim(memoryTrimType);
    assertSharedWithCount(KEYS[0], 100, 1);
    assertSharedWithCount(KEYS[1], 101, 1);
    assertSharedWithCount(KEYS[9], 109, 1);
    assertExclusivelyOwned(KEYS[4], 104);
    assertExclusivelyOwned(KEYS[5], 105);
    assertNotCached(KEYS[8], 108);
    assertNotCached(KEYS[2], 102);
    assertNotCached(KEYS[7], 107);
    assertNotCached(KEYS[3], 103);
    assertNotCached(KEYS[6], 106);
    assertTotalSize(5, 519);
    assertExclusivelyOwnedSize(2, 209);
    inOrder.verify(mReleaser).release(108);
    inOrder.verify(mReleaser).release(102);
    inOrder.verify(mReleaser).release(107);
    inOrder.verify(mReleaser).release(103);
    inOrder.verify(mReleaser).release(106);

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
    assertNotCached(KEYS[6], 104);
    assertNotCached(KEYS[6], 105);
    assertTotalSize(3, 310);
    assertExclusivelyOwnedSize(0, 0);
    inOrder.verify(mReleaser).release(104);
    inOrder.verify(mReleaser).release(105);
  }

  @Test
  public void testContains() {
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

  private CloseableReference<Integer> newReference(int size) {
    return CloseableReference.of(size, mReleaser);
  }

  private void assertSharedWithCount(String key, Integer value, int count) {
    assertTrue("key not found in the cache", mCache.mCachedEntries.contains(key));
    assertFalse("key found in the exclusives", mCache.mExclusiveEntries.contains(key));
    CountingMemoryCache.Entry<String, Integer> entry = mCache.mCachedEntries.get(key);
    assertNotNull("entry not found in the cache", entry);
    assertEquals("key mismatch", key, entry.key);
    assertEquals("value mismatch", value, entry.valueRef.get());
    assertEquals("client count mismatch", count, entry.clientCount);
    assertFalse("entry is an orphan", entry.isOrphan);
  }

  private void assertExclusivelyOwned(String key, Integer value) {
    assertTrue("key not found in the cache", mCache.mCachedEntries.contains(key));
    assertTrue("key not found in the exclusives", mCache.mExclusiveEntries.contains(key));
    CountingMemoryCache.Entry<String, Integer> entry = mCache.mCachedEntries.get(key);
    assertNotNull("entry not found in the cache", entry);
    assertEquals("key mismatch", key, entry.key);
    assertEquals("value mismatch", value, entry.valueRef.get());
    assertEquals("client count greater than zero", 0, entry.clientCount);
    assertFalse("entry is an orphan", entry.isOrphan);
  }

  private void assertNotCached(String key, Integer value) {
    assertFalse("key found in the cache", mCache.mCachedEntries.contains(key));
    assertFalse("key found in the exclusives", mCache.mExclusiveEntries.contains(key));
  }

  private void assertOrphanWithCount(CountingMemoryCache.Entry<String, Integer> entry, int count) {
    assertNotSame("entry found in the exclusives", entry, mCache.mCachedEntries.get(entry.key));
    assertNotSame("entry found in the cache", entry, mCache.mExclusiveEntries.get(entry.key));
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
}
