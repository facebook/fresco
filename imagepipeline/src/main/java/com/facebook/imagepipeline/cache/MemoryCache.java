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

import com.android.internal.util.Predicate;

public interface MemoryCache<K, V> {

  /**
   * Caches value with given key. Cache returns new copy of provided reference which should be used
   * instead of original one. Client should close returned reference when it is not required
   * anymore - just as it would close any other reference. If cache failed to cache given value
   * then null is returned.
   * @param key
   * @param value
   * @return cached copy of value
   */
  CloseableReference<V> cache(K key, CloseableReference<V> value);

  /**
   * Looks up cache for given key.
   * @param key
   * @return value to cached resource or null if no resource is cached
   */
  CloseableReference<V> get(K key);

  /**
   * Removes from the cache any keys that pass the specified predicate.
   * @param match determines which keys to remove
   * @return the number of entries that were removed
   */
  long removeAll(Predicate<K> match);
}
