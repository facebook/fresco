/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.disk

/**
 * A class which keeps a list of other classes to be notified of system disk events.
 *
 * If a class uses a lot of disk space and needs these notices from the system, it should implement
 * the [DiskTrimmable] interface.
 *
 * Implementations of this class should notify all the trimmables that have registered with it when
 * they need to trim their disk usage.
 */
interface DiskTrimmableRegistry {

  /** Register an object. */
  fun registerDiskTrimmable(trimmable: DiskTrimmable)

  /** Unregister an object. */
  fun unregisterDiskTrimmable(trimmable: DiskTrimmable)
}
