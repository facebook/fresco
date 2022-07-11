/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache.simple;

import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CountingLruMap;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
public class SimpleMemoryCache<K> implements CountingMemoryCache<K, CloseableImage> {

  private final Supplier<MemoryCacheParams> supplier;
  private final ImageLruCache<K> map;

  public SimpleMemoryCache(Supplier<MemoryCacheParams> memoryCacheParamsSupplier) {
    this.supplier = memoryCacheParamsSupplier;
    this.map = new ImageLruCache<K>(memoryCacheParamsSupplier.get().maxCacheSize);
  }

  /** @param observer ignored */
  @Nullable
  @Override
  public CloseableReference<CloseableImage> cache(
      K key,
      CloseableReference<CloseableImage> valueRef,
      @Nullable EntryStateObserver<K> observer) {
    int size = valueRef.get().getSizeInBytes();
    map.put(key, new SizedValue(valueRef, size));
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
    SizedValue value = map.get(key);
    if (value == null) {
      return null;
    }
    return value.value;
  }

  @Nullable
  @Override
  public CloseableImage inspect(K key) {
    return map.inspect(key);
  }

  @Override
  public void probe(K key) {
    map.get(key);
  }

  @Override
  public int removeAll(Predicate<K> predicate) {
    return map.removeAll(predicate);
  }

  @Override
  public boolean contains(Predicate<K> predicate) {
    return map.contains(predicate);
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
