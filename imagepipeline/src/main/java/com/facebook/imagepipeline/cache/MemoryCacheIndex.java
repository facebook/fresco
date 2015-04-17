/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.common.references.CloseableReference;

/**
 * Interface for classes that implement cache key to value mapping.
 *
 * <p>Since MemoryCacheIndex stores CloseableReferences, implementations must ensure
 * that those references remain valid for their whole lifetime in the cache.
 *
 * @param <K> type of key
 * @param <V> type of value
 */
public interface MemoryCacheIndex<K, V> {

  /**
   * Returns the value for the given key (if any).
   * @param key user specified key
   * @return value that should be returned by the cache.
   */
  CloseableReference<V> lookupValue(K key);

  /**
   * Called whenever new key value pair is added to the cache.
   * @param key
   * @param value
   * @return CloseableReference cached with the same key that should be removed from the cache
   */
  CloseableReference<V> addEntry(K key, CloseableReference<V> value);

  /**
   * Called whenever given value is removed from the cache
   * @param key
   * @param value
   */
  void removeEntry(K key, CloseableReference<V> value);
}
