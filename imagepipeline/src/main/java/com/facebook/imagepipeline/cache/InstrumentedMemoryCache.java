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
      mTracker.onCacheHit();
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
}
