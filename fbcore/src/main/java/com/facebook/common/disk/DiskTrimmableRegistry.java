/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.disk;

/**
 * A class which keeps a list of other classes to be notified of system disk events.
 *
 * <p>If a class uses a lot of disk space and needs these notices from the system, it should
 * implement the {@link DiskTrimmable} interface.
 *
 * <p>Implementations of this class should notify all the trimmables that have registered with it
 * when they need to trim their disk usage.
 */
public interface DiskTrimmableRegistry {

  /** Register an object. */
  void registerDiskTrimmable(DiskTrimmable trimmable);

  /** Unregister an object. */
  void unregisterDiskTrimmable(DiskTrimmable trimmable);
}
