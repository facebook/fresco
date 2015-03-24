/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.memory;

/**
 * Implementation of {@link MemoryTrimmableRegistry} that does not do anything.
 */
public class NoOpMemoryTrimmableRegistry implements MemoryTrimmableRegistry {
  private static NoOpMemoryTrimmableRegistry sInstance = null;

  private NoOpMemoryTrimmableRegistry() {
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
