/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

import com.facebook.common.references.ResourceReleaser;

/**
 * Manages a pool of reusable values of type V. The sizes of the values are described by the type S
 * The pool supports two main operations {@link #get(Object)} - returns a value of size that's the
 * same or larger than the requested size {@link #release(Object)} - releases the value to the pool
 *
 * <p>In addition, the pool subscribes to the memory manager, and responds to low-memory events via
 * calls to {@link com.facebook.common.memory.MemoryTrimmable#trim(MemoryTrimType)}. Some percent
 * (perhaps all) of the values in the pool are then 'freed'.
 *
 * <p>Known implementations: BasePool (GenericByteArrayPool, SingleByteArrayPool, BitmapPool)
 */
public interface Pool<V> extends ResourceReleaser<V>, MemoryTrimmable {

  /**
   * Gets a 'value' of size 'S' (or larger) from the pool, if available.
   * Allocates a new value if necessary.
   * @param size the logical size to allocate
   * @return a new value
   */
  V get(int size);

  /**
   * Releases the given value to the pool.
   * The pool may decide to
   *  - reuse the value (for future {@link #get(int)} operations OR
   *  - 'free' the value
   * @param value the value to release to the pool
   */
  void release(V value);
}
