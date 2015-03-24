/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.disk;

/**
 * Implementation of {@link DiskTrimmableRegistry} that does not do anything.
 */
public class NoOpDiskTrimmableRegistry implements DiskTrimmableRegistry {
  private static NoOpDiskTrimmableRegistry sInstance = null;

  private NoOpDiskTrimmableRegistry() {
  }

  public static synchronized NoOpDiskTrimmableRegistry getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpDiskTrimmableRegistry();
    }
    return sInstance;
  }

  @Override
  public void registerDiskTrimmable(DiskTrimmable trimmable) {
  }

  @Override
  public void unregisterDiskTrimmable(DiskTrimmable trimmable) {
  }
}
