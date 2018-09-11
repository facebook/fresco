/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to get pool stats
*/
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

  /**
   * Refresh all pool stats
   */
  public void refresh() {
    refreshBasic();
    refreshBucketStats();
  }

  public void refreshBasic() {
    mUsedBytes = mPool.mUsed.mNumBytes;
    mUsedCount = mPool.mUsed.mCount;
    mFreeBytes = mPool.mFree.mNumBytes;
    mFreeCount = mPool.mFree.mCount;
  }

  public void refreshBucketStats() {
    mBucketStats.clear();
    for (int i = 0; i < mPool.mBuckets.size(); ++i) {
      final int bucketedSize = mPool.mBuckets.keyAt(i);
      final Bucket<V> bucket = mPool.mBuckets.valueAt(i);
      mBucketStats.put(bucketedSize, new IntPair(bucket.getInUseCount(), bucket.getFreeListSize()));
    }
  }
}
