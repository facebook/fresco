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

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;

import com.android.internal.util.Predicate;

/**
 * Delegates cache and get methods to instance of CountingMemoryCache. Returned references are
 * wrapped in new instances of CloseableReferences with custom releaser that calls
 * release method of CountingMemoryCache
 */
public class ReferenceWrappingMemoryCache<K, V, S> implements MemoryCache<K, V, S> {

  private final CountingMemoryCache<K, V, S> mCountingMemoryCache;

  public ReferenceWrappingMemoryCache(final CountingMemoryCache<K, V, S> countingMemoryCache) {
    mCountingMemoryCache = Preconditions.checkNotNull(countingMemoryCache);
  }

  @Override
  public CloseableReference<V> get(K key, @Nullable S lookupStrategy) {
    return wrapCacheReferenceIfNotNull(key, mCountingMemoryCache.get(key, lookupStrategy));
  }

  @Override
  public CloseableReference<V> cache(final K key, final CloseableReference<V> value) {
    return wrapCacheReferenceIfNotNull(key, mCountingMemoryCache.cache(key, value));
  }

  @Override
  public long removeAll(Predicate<K> match) {
    return mCountingMemoryCache.removeAll(match);
  }

  private CloseableReference<V> wrapCacheReferenceIfNotNull(
      final K key,
      final @Nullable CloseableReference<V> value) {
    if (value == null) {
      return null;
    }

    return CloseableReference.of(value.get(), new ResourceReleaser<V>() {
          @Override
          public void release(V unused) {
            mCountingMemoryCache.release(key, value);
          }
        });
  }
}
