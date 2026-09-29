/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

/** Helper class to get pool stats */
class PoolStats<V : Any>(@JvmField var pool: BasePool<V>) {

  @JvmField var usedBytes: Int = 0

  @JvmField var usedCount: Int = 0

  @JvmField var freeBytes: Int = 0

  @JvmField var freeCount: Int = 0

  @JvmField var bucketStats: MutableMap<Int, IntPair> = HashMap()

  fun setPool(pool: BasePool<V>) {
    this.pool = pool
  }

  /** Refresh all pool stats */
  fun refresh() {
    refreshBasic()
    refreshBucketStats()
  }

  fun refreshBasic() {
    this.usedBytes = pool.used.numBytes
    this.usedCount = pool.used.count
    this.freeBytes = pool.free.numBytes
    this.freeCount = pool.free.count
  }

  fun refreshBucketStats() {
    bucketStats.clear()
    for (i in 0..<pool.buckets.size()) {
      val bucketedSize = pool.buckets.keyAt(i)
      val bucket = pool.buckets.valueAt(i)
      bucketStats[bucketedSize] = IntPair(bucket.inUseCount, bucket.freeListSize)
    }
  }

  fun getBucketStats(): Map<Int, IntPair> = bucketStats
}
