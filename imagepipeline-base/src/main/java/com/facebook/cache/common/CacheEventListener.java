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
 * An interface for logging various cache events.
 */
public interface CacheEventListener {

  /**
  * Triggered by a cache hit.
  */
  void onHit(CacheEvent cacheEvent);

  /**
   * Triggered by a cache miss for the given key.
   */
  void onMiss(CacheEvent cacheEvent);

  /**
   * Triggered at the start of the process to save a resource in cache.
   */
  void onWriteAttempt(CacheEvent cacheEvent);

  /**
   * Triggered after a resource has been successfully written to cache.
   */
  void onWriteSuccess(CacheEvent cacheEvent);

  /**
   * Triggered if a cache hit was attempted but an exception was thrown trying to read the resource
   * from storage.
   */
  void onReadException(CacheEvent cacheEvent);

  /**
   * Triggered if a cache write was attempted but an exception was thrown trying to write the
   * exception to storage.
   */
  void onWriteException(CacheEvent cacheEvent);

  /**
   * Triggered by an eviction from cache.
   */
  void onEviction(CacheEvent cacheEvent);

  enum EvictionReason {
    CACHE_FULL,
    CONTENT_STALE,
    USER_FORCED,
    CACHE_MANAGER_TRIMMED
  }
}
