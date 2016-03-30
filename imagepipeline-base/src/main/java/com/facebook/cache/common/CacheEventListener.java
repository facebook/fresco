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

  void onHit();

  void onMiss();

  void onWriteAttempt();

  void onReadException();

  void onWriteException();

  void onEviction(EvictionReason evictionReason, int itemCount, long itemSize);

  enum EvictionReason {
    CACHE_FULL,
    CONTENT_STALE,
    USER_FORCED,
    CACHE_MANAGER_TRIMMED
  }
}
