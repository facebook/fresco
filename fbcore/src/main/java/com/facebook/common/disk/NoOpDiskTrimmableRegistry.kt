/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.disk

/** Implementation of [DiskTrimmableRegistry] that does not do anything. */
object NoOpDiskTrimmableRegistry : DiskTrimmableRegistry {

  override fun registerDiskTrimmable(trimmable: DiskTrimmable) = Unit

  override fun unregisterDiskTrimmable(trimmable: DiskTrimmable) = Unit
}
