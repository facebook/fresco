/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

/** Listener that logs pool statistics. */
interface PoolStatsTracker {

  fun setBasePool(basePool: BasePool<*>)

  fun onValueReuse(bucketedSize: Int)

  fun onSoftCapReached()

  fun onHardCapReached()

  fun onAlloc(size: Int)

  fun onFree(sizeInBytes: Int)

  fun onValueRelease(sizeInBytes: Int)

  companion object {
    const val BUCKETS_USED_PREFIX = "buckets_used_"
    const val USED_COUNT = "used_count"
    const val USED_BYTES = "used_bytes"
    const val FREE_COUNT = "free_count"
    const val FREE_BYTES = "free_bytes"
    const val SOFT_CAP = "soft_cap"
    const val HARD_CAP = "hard_cap"
  }
}
