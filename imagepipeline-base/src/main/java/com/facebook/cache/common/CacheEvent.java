/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common;

import java.io.IOException;
import javax.annotation.Nullable;

/**
 * Interface to provide details of an individual cache event.
 *
 * <p> All values may be null depending on the kind of event. See the docs for each method to see
 * when to expect values to be available.
 */
public interface CacheEvent {

  /**
   * Gets the cache key related to this event.
   *
   * <p> This should be present for all events other than eviction.
   */
  @Nullable
  CacheKey getCacheKey();

  /**
   * Gets the resource ID for the cached item.
   *
   * <p> This is present in cache hit, write success, read and write exceptions and evictions.
   *
   * <p> It may also be present in cache miss events if an ID was found in the cache's index but the
   * resource wasn't then found in storage.
   */
  @Nullable
  String getResourceId();

  /**
   * Gets the size of the new resource in storage, in bytes.
   *
   * <p> This is present in write success and eviction events.
   */
  long getItemSize();

  /**
   * Gets the total size of the resources currently in storage, in bytes.
   *
   * <p> This is present in write success and eviction events.
   */
  long getCacheSize();

  /**
   * Gets the current size limit for the cache, in bytes.
   *
   * <p> This is present in eviction events where the eviction is due to the need to trim for size.
   */
  long getCacheLimit();

  /**
   * Gets the exception which occurred to trigger a read or write exception event.
   */
  @Nullable
  IOException getException();

  /**
   * Gets the reason for an item's eviction in eviction events.
   */
  @Nullable
  CacheEventListener.EvictionReason getEvictionReason();
}
