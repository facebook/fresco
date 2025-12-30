/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import java.util.HashMap;
import java.util.Map;

/** Helper class to get pool stats */
public class PoolStats<V> {
  public BasePool<V> pool;

  public int usedBytes;
  public int usedCount;
  public int freeBytes;
  public int freeCount;

  Map<Integer, IntPair> bucketStats;

  public PoolStats(BasePool<V> pool) {
    this.pool = pool;
    bucketStats = new HashMap<>();
  }

  public void setPool(BasePool<V> pool) {
    this.pool = pool;
  }

  /** Refresh all pool stats */
  public void refresh() {
    refreshBasic();
    refreshBucketStats();
  }

  public void refreshBasic() {
    usedBytes = pool.used.numBytes;
    usedCount = pool.used.count;
    freeBytes = pool.free.numBytes;
    freeCount = pool.free.count;
  }

  public void refreshBucketStats() {
    bucketStats.clear();
    for (int i = 0; i < pool.buckets.size(); ++i) {
      final int bucketedSize = pool.buckets.keyAt(i);
      final Bucket<V> bucket = pool.buckets.valueAt(i);
      bucketStats.put(bucketedSize, new IntPair(bucket.getInUseCount(), bucket.getFreeListSize()));
    }
  }

  Map<Integer, IntPair> getBucketStats() {
    return bucketStats;
  }
}
