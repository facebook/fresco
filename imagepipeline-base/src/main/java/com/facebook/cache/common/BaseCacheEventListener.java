/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
