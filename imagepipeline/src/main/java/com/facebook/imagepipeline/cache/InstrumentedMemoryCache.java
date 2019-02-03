/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.common.internal.Predicate;
import com.facebook.common.references.CloseableReference;

public class InstrumentedMemoryCache<K, V> implements MemoryCache<K, V> {

  private final MemoryCache<K, V> mDelegate;
  private final MemoryCacheTracker mTracker;

  public InstrumentedMemoryCache(MemoryCache<K, V> delegate, MemoryCacheTracker tracker) {
    mDelegate = delegate;
    mTracker = tracker;
  }

  @Override
  public CloseableReference<V> get(K key) {
    CloseableReference<V> result = mDelegate.get(key);
    if (result == null) {
      mTracker.onCacheMiss();
    } else {
      mTracker.onCacheHit(key);
    }
    return result;
  }

  @Override
  public CloseableReference<V> cache(K key, CloseableReference<V> value) {
    mTracker.onCachePut();
    return mDelegate.cache(key, value);
  }

  @Override
  public int removeAll(Predicate<K> predicate) {
    return mDelegate.removeAll(predicate);
  }

  @Override
  public boolean contains(Predicate<K> predicate) {
    return mDelegate.contains(predicate);
  }

  @Override
  public boolean contains(K key) {
    return mDelegate.contains(key);
  }
}
