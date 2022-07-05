/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache.simple;

import java.util.Map;
import java.util.Set;

class ImageLruCache<K> extends ExtendedLruCache<K, SizedEntry> {

  /**
   * @param maxSize for caches that do not override {@link #sizeOf}, this is the maximum number of
   *     entries in the cache. For all other caches, this is the maximum sum of the sizes of the
   *     entries in this cache.
   */
  public ImageLruCache(int maxSize) {
    super(maxSize);
  }

  @Override
  protected int sizeOf(K key, SizedEntry value) {
    return value.size;
  }

  public Set<K> keys() {
    return map.keySet();
  }

  public Set<Map.Entry<K, SizedEntry>> entries() {
    return map.entrySet();
  }

  /** @return number of elements currently in cache */
  public synchronized int count() {
    return putCount() - evictionCount();
  }
}
