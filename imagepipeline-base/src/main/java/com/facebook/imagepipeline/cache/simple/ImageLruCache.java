/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache.simple;

import com.facebook.common.internal.Predicate;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
final class ImageLruCache<K> extends ExtendedLruCache<K, SizedValue> {

  /**
   * @param maxSize for caches that do not override {@link #sizeOf}, this is the maximum number of
   *     entries in the cache. For all other caches, this is the maximum sum of the sizes of the
   *     entries in this cache.
   */
  public ImageLruCache(int maxSize) {
    super(maxSize);
  }

  @Override
  protected int sizeOf(K key, SizedValue value) {
    return value.getSize();
  }

  /** @return number of elements currently in cache */
  public synchronized int count() {
    return putCount() - evictionCount();
  }

  public synchronized int removeAll(Predicate<K> predicate) {
    int count = 0;
    while (true) {
      K key;
      SizedValue value;
      boolean evict;
      synchronized (this) {
        if (size < 0 || (map.isEmpty() && size != 0)) {
          throw new IllegalStateException(
              getClass().getName() + ".sizeOf() is reporting inconsistent results!");
        }

        Iterator<Map.Entry<K, SizedValue>> iter = map.entrySet().iterator();
        if (!iter.hasNext()) {
          break;
        }
        Map.Entry<K, SizedValue> toEvict = iter.next();
        key = toEvict.getKey();
        value = toEvict.getValue();
        evict = predicate.apply(key);
        if (evict) {
          map.remove(key);
          size -= safeSizeOf(key, value);
          evictionCount++;
        }
      }

      if (evict) {
        entryRemoved(true, key, value, null);
      }
    }
    return count;
  }

  public synchronized boolean contains(Predicate<K> predicate) {
    for (K key : map.keySet()) {
      if (predicate.apply(key)) {
        return true;
      }
    }
    return false;
  }

  public synchronized @Nullable CloseableImage inspect(K key) {
    for (Map.Entry<K, SizedValue> entry : map.entrySet()) {
      if (entry.getKey().equals(key)) {
        CloseableReference<CloseableImage> ref = entry.getValue().getValue();
        return ref.get();
      }
    }
    return null;
  }
}
