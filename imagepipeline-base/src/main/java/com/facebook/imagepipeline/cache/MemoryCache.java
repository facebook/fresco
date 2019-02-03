/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.common.internal.Predicate;
import com.facebook.common.references.CloseableReference;
import javax.annotation.Nullable;

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
  CloseableReference<V> cache(K key, CloseableReference<V> value);

  /**
   * Gets the item with the given key, or null if there is no such item.
   *
   * @param key
   * @return a reference to the cached value, or null if the item was not found
   */
  @Nullable
  CloseableReference<V> get(K key);

  /**
   * Removes all the items from the cache whose keys match the specified predicate.
   *
   * @param predicate returns true if an item with the given key should be removed
   * @return number of the items removed from the cache
   */
  int removeAll(Predicate<K> predicate);

  /**
   * Find if any of the items from the cache whose keys match the specified predicate.
   *
   * @param predicate returns true if an item with the given key matches
   * @return true if the predicate was found in the cache, false otherwise
   */
  boolean contains(Predicate<K> predicate);

  /**
   * Check if the cache contains an item for the given key.
   *
   * @param key
   * @return true if the key was found in the cache, false otherwise
   */
  boolean contains(K key);
}
