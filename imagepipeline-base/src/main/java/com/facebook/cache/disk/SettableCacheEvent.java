/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

import javax.annotation.Nullable;

import java.io.IOException;

import com.facebook.cache.common.CacheEvent;
import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.CacheKey;

/**
 * Implementation of {@link CacheEvent} that allows the values to be set.
 */
public class SettableCacheEvent implements CacheEvent {

  private CacheKey mCacheKey;
  private String mResourceId;
  private long mItemSize;
  private long mCacheLimit;
  private long mCacheSize;
  private IOException mException;
  private CacheEventListener.EvictionReason mEvictionReason;

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
}
