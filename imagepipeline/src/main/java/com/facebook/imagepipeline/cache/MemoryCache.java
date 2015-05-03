/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import javax.annotation.Nullable;

import com.facebook.common.references.CloseableReference;

import com.android.internal.util.Predicate;

/**
 * Interface for the image pipeline memory cache.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface MemoryCache<K, V> {

  /**
   * Caches the the given key-value pair.
   *
   * <p> The cache returns a new copy of the provided reference which should be used instead of the
   * original one. The client should close the returned reference when it is not required anymore.
   *
   * <p> If the cache failed to cache the given value, then the null reference is returned.
   *
   * @param key
   * @param value
   * @return a new reference to be used, or null if the caching failed
   */
  @Nullable
  public CloseableReference<V> cache(K key, CloseableReference<V> value);

  /**
   * Gets the item with the given key, or null if there is no such item.
   *
   * @param key
   * @return a reference to the cached value, or null if the item was not found
   */
  @Nullable
  public CloseableReference<V> get(K key);

  /**
   * Removes all the items from the cache whose keys match the specified predicate.
   *
   * @param predicate returns true if an item with the given key should be removed
   * @return number of the items removed from the cache
   */
  public int removeAll(Predicate<K> predicate);
}
