/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import javax.annotation.Nullable;

/**
 * Implementation of {@link CacheEventListener} that doesn't do anything.
 */
public class NoOpCacheEventListener implements CacheEventListener {
  private static @Nullable NoOpCacheEventListener sInstance = null;

  private NoOpCacheEventListener() {
  }

  public static synchronized NoOpCacheEventListener getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpCacheEventListener();
    }
    return sInstance;
  }

  @Override
  public void onHit(CacheEvent cacheEvent) {

  }

  @Override
  public void onMiss(CacheEvent cacheEvent) {
  }

  @Override
  public void onWriteAttempt(CacheEvent cacheEvent) {
  }

  @Override
  public void onWriteSuccess(CacheEvent cacheEvent) {
  }

  @Override
  public void onReadException(CacheEvent cacheEvent) {
  }

  @Override
  public void onWriteException(CacheEvent cacheEvent) {
  }

  @Override
  public void onEviction(CacheEvent cacheEvent) {
  }

  @Override
  public void onCleared() {
  }
}
