/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.common

/** Implementation of [CacheEventListener] that doesn't do anything. */
class NoOpCacheEventListener private constructor() : CacheEventListener {

  override fun onHit(cacheEvent: CacheEvent) = Unit

  override fun onMiss(cacheEvent: CacheEvent) = Unit

  override fun onWriteAttempt(cacheEvent: CacheEvent) = Unit

  override fun onWriteSuccess(cacheEvent: CacheEvent) = Unit

  override fun onReadException(cacheEvent: CacheEvent) = Unit

  override fun onWriteException(cacheEvent: CacheEvent) = Unit

  override fun onEviction(cacheEvent: CacheEvent) = Unit

  override fun onCleared() = Unit

  companion object {
    private var _instance: NoOpCacheEventListener? = null

    @get:JvmStatic
    @get:Synchronized
    val instance: NoOpCacheEventListener?
      get() {
        if (_instance == null) {
          _instance = NoOpCacheEventListener()
        }
        return _instance
      }
  }
}
