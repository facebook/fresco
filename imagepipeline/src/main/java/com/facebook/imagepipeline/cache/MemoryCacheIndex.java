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
 * Interface for classes that implement cache key to value mapping. The class is free to permit
 * multiple values per key, and clients doing a lookup provide a strategy alongside the key
 * which is used to determine which of the values to return.
 *
 * <p> Implementations may choose to restrict the number of values associated with each key
 * ({@see MemoryCacheLookupAlgorithm.addEntry}).
 *
 * <p>Since MemoryCacheIndex stores CloseableReferences, implementations must ensure
 * that those references remain valid for their whole lifetime in the cache.
 *
 * @param <K> type of key
 * @param <V> type of value
 * @param <S> type of additional lookup strategy parameter
 */
public interface MemoryCacheIndex<K, V, S> {

  /**
   * Determines which value (if any) should be returned by the cache for given client request.
   * @param key user specified key
   * @param lookupStrategy strategy specifying additional requirements for returned value
   * @return value that should be returned by the cache.
   */
  CloseableReference<V> lookupValue(K key, S lookupStrategy);

  /**
   * Called whenever new key value pair is added to the cache. It might be necessary to remove
   * some other value so that algorithm does not break
   * @param key
   * @param value
   * @return CloseableReference cached with the same key that should be removed from the cache
   */
  CloseableReference<V> addEntry(K key, CloseableReference<V> value);

  /**
   * Called whenever given value is removed from the cache (unless the value is the one determined
   * by return value of addEntry)
   * @param key
   * @param value
   */
  void removeEntry(K key, CloseableReference<V> value);
}
