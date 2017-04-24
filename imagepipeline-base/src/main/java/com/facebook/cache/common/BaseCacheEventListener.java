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
 * No-op implementation of {@link CacheEventListener} so that listeners can extend and implement
 * only the events they're interested in.
 */
public class BaseCacheEventListener implements CacheEventListener {

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
