/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Relies on {@link BucketMap} to implement pooling Items from the bucket with LRU key will be
 * removed first
 *
 * @param <T>
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public abstract class LruBucketsPoolBackend<T> implements PoolBackend<T> {

  private final Set<T> mCurrentItems = new HashSet<>();
  private final BucketMap<T> mMap = new BucketMap<>();

  public LruBucketsPoolBackend() {}

  @Override
  @Nullable
  public T get(int size) {
    return maybeRemoveFromCurrentItems(mMap.acquire(size));
  }

  @Override
  public void put(T item) {
    boolean wasAdded;
    synchronized (this) {
      wasAdded = mCurrentItems.add(item);
    }
    if (wasAdded) {
      mMap.release(getSize(item), item);
    }
  }

  @Override
  @Nullable
  public T pop() {
    return maybeRemoveFromCurrentItems(mMap.removeFromEnd());
  }

  private @Nullable T maybeRemoveFromCurrentItems(@Nullable T t) {
    if (t != null) {
      synchronized (this) {
        mCurrentItems.remove(t);
      }
    }
    return t;
  }

  @VisibleForTesting
  int valueCount() {
    return mMap.valueCount();
  }
}
