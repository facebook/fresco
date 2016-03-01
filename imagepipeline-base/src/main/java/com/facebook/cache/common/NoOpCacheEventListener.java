/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.common;

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
  public void onHit() {

  }

  @Override
  public void onMiss() {
  }

  @Override
  public void onWriteAttempt() {
  }

  @Override
  public void onReadException() {
  }

  @Override
  public void onWriteException() {
  }

  @Override
  public void onEviction(EvictionReason evictionReason, int itemCount, long itemSize) {
  }
}
