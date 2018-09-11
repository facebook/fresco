/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import com.facebook.cache.common.CacheEvent;
import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.CacheKey;
import com.facebook.infer.annotation.ReturnsOwnership;
import java.io.IOException;
import javax.annotation.Nullable;

/**
 * Implementation of {@link CacheEvent} that allows the values to be set and supports recycling of
 * instances.
 */
public class SettableCacheEvent implements CacheEvent {

  private static final Object RECYCLER_LOCK = new Object();
  private static final int MAX_RECYCLED = 5;

  private static SettableCacheEvent sFirstRecycledEvent;
  private static int sRecycledCount;

  private CacheKey mCacheKey;
  private String mResourceId;
  private long mItemSize;
  private long mCacheLimit;
  private long mCacheSize;
  private IOException mException;
  private CacheEventListener.EvictionReason mEvictionReason;
  private SettableCacheEvent mNextRecycledEvent;

  @ReturnsOwnership
  public static SettableCacheEvent obtain() {
    synchronized (RECYCLER_LOCK) {
      if (sFirstRecycledEvent != null) {
        SettableCacheEvent eventToReuse = sFirstRecycledEvent;
        sFirstRecycledEvent = eventToReuse.mNextRecycledEvent;
        eventToReuse.mNextRecycledEvent = null;
        sRecycledCount--;
        return eventToReuse;
      }
    }

    return new SettableCacheEvent();
  }

  private SettableCacheEvent() {
  }

  @Nullable
  @Override
  public CacheKey getCacheKey() {
    return mCacheKey;
  }

  public SettableCacheEvent setCacheKey(CacheKey cacheKey) {
    mCacheKey = cacheKey;
    return this;
  }

  @Nullable
  @Override
  public String getResourceId() {
    return mResourceId;
  }

  public SettableCacheEvent setResourceId(String resourceId) {
    mResourceId = resourceId;
    return this;
  }

  @Override
  public long getItemSize() {
    return mItemSize;
  }

  public SettableCacheEvent setItemSize(long itemSize) {
    mItemSize = itemSize;
    return this;
  }

  @Override
  public long getCacheSize() {
    return mCacheSize;
  }

  public SettableCacheEvent setCacheSize(long cacheSize) {
    mCacheSize = cacheSize;
    return this;
  }

  @Override
  public long getCacheLimit() {
    return mCacheLimit;
  }

  public SettableCacheEvent setCacheLimit(long cacheLimit) {
    mCacheLimit = cacheLimit;
    return this;
  }

  @Nullable
  @Override
  public IOException getException() {
    return mException;
  }

  public SettableCacheEvent setException(IOException exception) {
    mException = exception;
    return this;
  }

  @Nullable
  @Override
  public CacheEventListener.EvictionReason getEvictionReason() {
    return mEvictionReason;
  }

  public SettableCacheEvent setEvictionReason(CacheEventListener.EvictionReason evictionReason) {
    mEvictionReason = evictionReason;
    return this;
  }

  public void recycle() {
    synchronized (RECYCLER_LOCK) {
      if (sRecycledCount < MAX_RECYCLED) {
        reset();
        sRecycledCount++;

        if (sFirstRecycledEvent != null) {
          mNextRecycledEvent = sFirstRecycledEvent;
        }
        sFirstRecycledEvent = this;
      }
    }
  }

  private void reset() {
    mCacheKey = null;
    mResourceId = null;
    mItemSize = 0;
    mCacheLimit = 0;
    mCacheSize = 0;
    mException = null;
    mEvictionReason = null;
  }
}
