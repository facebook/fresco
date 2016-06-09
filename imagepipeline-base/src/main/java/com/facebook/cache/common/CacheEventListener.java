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
 * An interface for logging various cache events.
 */
public interface CacheEventListener {

  /**
   * Triggered by a cache hit for the given key.
   *
   * @param key the cache key
   * @param resourceId the resource ID of the matching resource in cache
   */
  void onHit(CacheKey key, String resourceId);

  /**
   * Triggered by a cache miss for the given key.
   *
   * @param key the cache key
   * @param resourceId the resource ID, if one was found. This will normally be null and if not
   * would suggest a resource has previously been cached for the key but is no longer available.
   */
  void onMiss(CacheKey key, @Nullable String resourceId);

  /**
   * Triggered at the start of the process to save a resource in cache.
   *
   * @param key the cache key
   */
  void onWriteAttempt(CacheKey key);

  /**
   * Triggered after a resource has been successfully written to cache.
   *
   * @param key the cache key
   * @param resourceId the new resource ID, which will also be returned in later events for the same
   * resource
   * @param itemSize size of the new resource in storage
   */
  void onWriteSuccess(CacheKey key, String resourceId, long itemSize);

  /**
   * Triggered if a cache hit was attempted but an exception was thrown trying to read the resource
   * from storage.
   *
   * @param key the cache key
   * @param resourceId ID of the resource which was attempted to be read, which may be null if the
   * exception was thrown before even this was known
   * @param e the exception
   */
  void onReadException(CacheKey key, @Nullable String resourceId, IOException e);

  /**
   * Triggered if a cache write was attempted but an exception was thrown trying to write the
   * exception to storage.
   *
   * @param key the cache key
   * @param resourceId ID of the new resource for which writing was attempted
   * @param e the exception
   */
  void onWriteException(CacheKey key, String resourceId, IOException e);

  /**
   * Triggered by an eviction from cache.
   *
   * <p>The cache key is not available (or relevant) at this point but the resource ID will match
   * previous events for the same resource
   *
   * @param resourceId ID of the evicted resource
   * @param evictionReason why the eviction occurred
   * @param itemSize size of the evicted resource in storage
   */
  void onEviction(String resourceId, EvictionReason evictionReason, long itemSize);

  enum EvictionReason {
    CACHE_FULL,
    CONTENT_STALE,
    USER_FORCED,
    CACHE_MANAGER_TRIMMED
  }
}
