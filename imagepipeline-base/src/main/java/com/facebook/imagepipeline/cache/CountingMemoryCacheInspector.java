/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inspects values cached in bitmap memory cache. */
public class CountingMemoryCacheInspector<K, V> {

  /** Cache entry info for use by dumpers. */
  public static class DumpInfoEntry<K, V> {
    // The key is immutable, so it's safe to store that directly
    public final K key;

    // The value
    public final CloseableReference<V> value;

    public DumpInfoEntry(final K key, final CloseableReference<V> valueRef) {
      this.key = Preconditions.checkNotNull(key);
      this.value = CloseableReference.cloneOrNull(valueRef);
    }

    public void release() {
      CloseableReference.closeSafely(value);
    }
  }

  /** Info about the status of the cache for use by dumpers. */
  public static class DumpInfo<K, V> {
    public final int maxSize;
    public final int maxEntriesCount;
    public final int maxEntrySize;

    public final int size;
    public final int lruSize;

    public final List<DumpInfoEntry<K, V>> lruEntries;
    public final List<DumpInfoEntry<K, V>> sharedEntries;
    public final Map<Bitmap, Object> otherEntries;

    public DumpInfo(int size, int lruSize, MemoryCacheParams params) {
      maxSize = params.maxCacheSize;
      maxEntriesCount = params.maxCacheEntries;
      maxEntrySize = params.maxCacheEntrySize;

      this.size = size;
      this.lruSize = lruSize;

      lruEntries = new ArrayList<>();
      sharedEntries = new ArrayList<>();
      otherEntries = new HashMap<>();
    }

    public void release() {
      for (DumpInfoEntry entry : lruEntries) {
        entry.release();
      }
      for (DumpInfoEntry entry : sharedEntries) {
        entry.release();
      }
    }
  }

  private final CountingMemoryCache<K, V> mCountingBitmapCache;

  public CountingMemoryCacheInspector(CountingMemoryCache<K, V> countingBitmapCache) {
    mCountingBitmapCache = countingBitmapCache;
  }

  /**
   * Iterates through all entries cached in counting cache and builds snapshot of its content. This
   * should be used by tools that need to know current content of given cache.
   *
   * <p>Caller should call release method on returned DumpInfo after it is done with examining cache
   * contents
   */
  public DumpInfo dumpCacheContent() {
    synchronized (mCountingBitmapCache) {
      DumpInfo<K, V> dumpInfo =
          new DumpInfo<>(
              mCountingBitmapCache.getSizeInBytes(),
              mCountingBitmapCache.getEvictionQueueSizeInBytes(),
              mCountingBitmapCache.getMemoryCacheParams());

      final List<LinkedHashMap.Entry<K, CountingMemoryCache.Entry<K, V>>> cachedEntries =
          mCountingBitmapCache.getCachedEntries().getMatchingEntries(null);
      for (LinkedHashMap.Entry<K, CountingMemoryCache.Entry<K, V>> cachedEntry : cachedEntries) {
        CountingMemoryCache.Entry<K, V> entry = cachedEntry.getValue();
        DumpInfoEntry<K, V> dumpEntry = new DumpInfoEntry<>(entry.key, entry.valueRef);
        if (entry.clientCount > 0) {
          dumpInfo.sharedEntries.add(dumpEntry);
        } else {
          dumpInfo.lruEntries.add(dumpEntry);
        }
      }
      for (Map.Entry<Bitmap, Object> entry : mCountingBitmapCache.getOtherEntries().entrySet()) {
        if (entry != null && !entry.getKey().isRecycled()) {
          dumpInfo.otherEntries.put(entry.getKey(), entry.getValue());
        }
      }

      return dumpInfo;
    }
  }
}
