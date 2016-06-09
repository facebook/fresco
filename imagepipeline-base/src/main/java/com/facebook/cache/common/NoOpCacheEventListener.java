/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.common;

import javax.annotation.Nullable;

import java.io.IOException;

/**
 * Implementation of {@link CacheEventListener} that doesn't do anything.
 */
public class NoOpCacheEventListener implements CacheEventListener {
  private static NoOpCacheEventListener sInstance = null;

  private NoOpCacheEventListener() {
  }

  public static synchronized NoOpCacheEventListener getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpCacheEventListener();
    }
    return sInstance;
  }

  @Override
  public void onHit(CacheKey key, String resourceId) {

  }

  @Override
  public void onMiss(CacheKey key, @Nullable String resourceId) {
  }

  @Override
  public void onWriteAttempt(CacheKey key) {
  }

  @Override
  public void onWriteSuccess(CacheKey key, String resourceId, long itemSize) {
  }

  @Override
  public void onReadException(CacheKey key, @Nullable String resourceId, IOException e) {
  }

  @Override
  public void onWriteException(CacheKey key, String resourceId, IOException e) {
  }

  @Override
  public void onEviction(String resourceId, EvictionReason evictionReason, long itemSize) {
  }
}
