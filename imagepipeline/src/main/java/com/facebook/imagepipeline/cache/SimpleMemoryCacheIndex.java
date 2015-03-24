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

import java.util.Map;

import com.facebook.common.internal.Maps;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;

/**
 * Basic implementation of MemoryCacheIndex.
 * @param <K>
 * @param <V>
 */
public class SimpleMemoryCacheIndex<K, V> implements MemoryCacheIndex<K, V, Void> {

  private final Map<K, CloseableReference<V>> mMapIndex;

  public SimpleMemoryCacheIndex() {
    mMapIndex = Maps.newHashMap();
  }

  @Override
  public synchronized CloseableReference<V> addEntry(
      final K key,
      final CloseableReference<V> value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);

    return mMapIndex.put(key, value);
  }

  @Override
  public synchronized CloseableReference<V> lookupValue(final K key, final @Nullable Void unused) {
    Preconditions.checkNotNull(key);
    return mMapIndex.get(key);
  }

  @Override
  public synchronized void removeEntry(final K key, final CloseableReference<V> value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);
    Preconditions.checkState(mMapIndex.remove(key) == value);
  }
}
