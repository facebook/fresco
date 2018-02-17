/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

/**
 * A class which keeps a list of other classes to be notified of system memory events.
 *
 * <p>If a class uses a lot of memory and needs these notices from the system, it should implement
 * the {@link MemoryTrimmable} interface.
 *
 * <p>Implementations of this class should notify all the trimmables that have registered with it
 * when they need to trim their memory usage.
 */
public interface MemoryTrimmableRegistry {

  /** Register an object. */
  void registerMemoryTrimmable(MemoryTrimmable trimmable);

  /** Unregister an object. */
  void unregisterMemoryTrimmable(MemoryTrimmable trimmable);
}
