/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

/**
 * Implementation of {@link MemoryTrimmableRegistry} that does not do anything.
 */
public class NoOpMemoryTrimmableRegistry implements MemoryTrimmableRegistry {
  private static NoOpMemoryTrimmableRegistry sInstance = null;

  public NoOpMemoryTrimmableRegistry() {
  }

  public static synchronized NoOpMemoryTrimmableRegistry getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpMemoryTrimmableRegistry();
    }
    return sInstance;
  }

  /** Register an object. */
  public void registerMemoryTrimmable(MemoryTrimmable trimmable) {
  }

  /** Unregister an object. */
  public void unregisterMemoryTrimmable(MemoryTrimmable trimmable) {
  }
}
