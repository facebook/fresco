/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

/**
 * An interface for logging various cache events.
 *
 * <p> In all callback methods, the {@link CacheEvent} object should not be held beyond the method
 * itself as they may be automatically recycled.
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

  /**
   * Triggered by a full cache clearance.
   */
  void onCleared();

  enum EvictionReason {
    CACHE_FULL,
    CONTENT_STALE,
    USER_FORCED,
    CACHE_MANAGER_TRIMMED
  }
}
