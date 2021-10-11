/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class SimpleMemoryCache<K> implements CountingMemoryCache<K, CloseableImage> {

  private static class Entry {
    final CloseableReference<CloseableImage> value;
    final int size;

    public Entry(CloseableReference<CloseableImage> value, int size) {
      this.value = value;
      this.size = size;
    }
  }

  private class ImageLruCache extends ExtendedLruCache<K, Entry> {

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is the maximum number of
     *     entries in the cache. For all other caches, this is the maximum sum of the sizes of the
     *     entries in this cache.
     */
    public ImageLruCache(int maxSize) {
      super(maxSize);
    }

    @Override
    protected int sizeOf(K key, Entry value) {
      return value.size;
    }

    public Set<K> keys() {
      return map.keySet();
    }

    public Set<Map.Entry<K, Entry>> entries() {
      return map.entrySet();
    }

    /** @return number of elements currently in cache */
    public synchronized int count() {
      return putCount() - evictionCount();
    }
  }

  private final Supplier<MemoryCacheParams> supplier;
  private final ImageLruCache map;

  public SimpleMemoryCache(Supplier<MemoryCacheParams> memoryCacheParamsSupplier) {
    this.supplier = memoryCacheParamsSupplier;
    this.map = new ImageLruCache(memoryCacheParamsSupplier.get().maxCacheSize);
  }

  /** @param observer ignored */
  @Nullable
  @Override
  public CloseableReference<CloseableImage> cache(
      K key,
      CloseableReference<CloseableImage> valueRef,
      @Nullable EntryStateObserver<K> observer) {
    int size = valueRef.get().getSizeInBytes();
    map.put(key, new Entry(valueRef, size));
    return valueRef;
  }

  @Nullable
  @Override
  public CloseableReference<CloseableImage> reuse(K key) {
    return get(key);
  }

  @Nullable
  @Override
  public CloseableReference<CloseableImage> cache(
      K key, CloseableReference<CloseableImage> valueRef) {
    return cache(key, valueRef, null);
  }

  @Override
  public void maybeEvictEntries() {}

  @Override
  public int getInUseSizeInBytes() {
    return map.size();
  }

  @Override
  public int getEvictionQueueCount() {
    return 0;
  }

  @Override
  public int getEvictionQueueSizeInBytes() {
    return 0;
  }

  @Override
  public void clear() {
    map.evictAll();
  }

  @Override
  public MemoryCacheParams getMemoryCacheParams() {
    return supplier.get();
  }

  @Override
  public @Nullable CountingLruMap<K, CountingMemoryCache.Entry<K, CloseableImage>>
      getCachedEntries() {
    return null;
  }

  @Override
  public @Nullable Map<Bitmap, Object> getOtherEntries() {
    return null;
  }

  @Nullable
  @Override
  public CloseableReference<CloseableImage> get(K key) {
    Entry entry = map.get(key);
    if (entry == null) {
      return null;
    }
    return entry.value;
  }

  @Nullable
  @Override
  public CloseableImage inspect(K key) {
    for (Map.Entry<K, Entry> entry : map.entries()) {
      if (entry.getKey().equals(key)) {
        CloseableReference<CloseableImage> ref = entry.getValue().value;
        return ref.get();
      }
    }
    return null;
  }

  @Override
  public void probe(K key) {
    map.get(key);
  }

  @Override
  public int removeAll(Predicate<K> predicate) {
    int count = 0;
    for (K key : map.keys()) {
      if (predicate.apply(key)) {
        map.remove(key);
        count++;
      }
    }
    return count;
  }

  @Override
  public boolean contains(Predicate<K> predicate) {
    for (K key : map.map.keySet()) {
      if (predicate.apply(key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(K key) {
    return map.get(key) != null;
  }

  @Override
  public synchronized int getCount() {
    return map.count();
  }

  @Override
  public int getSizeInBytes() {
    return map.size();
  }

  @Nullable
  @Override
  public String getDebugData() {
    return null;
  }

  @Override
  public void trim(MemoryTrimType trimType) {
    int size = (int) (trimType.getSuggestedTrimRatio() * (double) map.size());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      map.trimToSize(size);
    } else {
      map.evictAll();
    }
  }
}
