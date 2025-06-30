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
  public BasePool<V> mPool;

  public int mUsedBytes;
  public int mUsedCount;
  public int mFreeBytes;
  public int mFreeCount;

  Map<Integer, IntPair> mBucketStats;

  public PoolStats(BasePool<V> pool) {
    mPool = pool;
    mBucketStats = new HashMap<>();
  }

  public void setPool(BasePool<V> pool) {
    mPool = pool;
  }

  /** Refresh all pool stats */
  public void refresh() {
    refreshBasic();
    refreshBucketStats();
  }

  public void refreshBasic() {
    mUsedBytes = mPool.used.numBytes;
    mUsedCount = mPool.used.count;
    mFreeBytes = mPool.free.numBytes;
    mFreeCount = mPool.free.count;
  }

  public void refreshBucketStats() {
    mBucketStats.clear();
    for (int i = 0; i < mPool.buckets.size(); ++i) {
      final int bucketedSize = mPool.buckets.keyAt(i);
      final Bucket<V> bucket = mPool.buckets.valueAt(i);
      mBucketStats.put(bucketedSize, new IntPair(bucket.getInUseCount(), bucket.getFreeListSize()));
    }
  }

  Map<Integer, IntPair> getBucketStats() {
    return mBucketStats;
  }
}
