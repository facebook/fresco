/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Manages pooled objects
 *
 * @param <T> type of pooled objects
 */
@Nullsafe(Nullsafe.Mode.STRICT)
interface PoolBackend<T> {
  /** @return available object from the pool or null */
  @Nullable
  T get(int size);

  void put(T item);

  /** @return size for item which will be used in {@link #get(int)} */
  int getSize(T item);

  /**
   * Removed a single object (if any) from the pool
   *
   * @return the removed object or null
   */
  @Nullable
  T pop();
}
