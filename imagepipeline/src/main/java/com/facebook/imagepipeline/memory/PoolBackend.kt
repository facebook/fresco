/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

/**
 * Manages pooled objects
 *
 * @param <T> type of pooled objects </T>
 */
internal interface PoolBackend<T> {

  /** @return available object from the pool or null */
  operator fun get(size: Int): T?

  fun put(item: T)

  /** @return size for item which will be used in [get(int)] */
  fun getSize(item: T): Int

  /**
   * Removed a single object (if any) from the pool
   *
   * @return the removed object or null
   */
  fun pop(): T?
}
