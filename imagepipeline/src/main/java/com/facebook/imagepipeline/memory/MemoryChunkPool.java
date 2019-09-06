/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import android.util.SparseIntArray;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import javax.annotation.concurrent.ThreadSafe;

/** Manages a pool of memory chunks ({@link MemoryChunk}) */
@ThreadSafe
public abstract class MemoryChunkPool extends BasePool<MemoryChunk> {
  private final int[] mBucketSizes;

  /**
   * Initialize a new instance of the MemoryChunkPool
   *
   * @param memoryTrimmableRegistry the memory manager to register with
   * @param poolParams provider for pool parameters
   * @param memoryChunkPoolStatsTracker the pool stats tracker
   */
  protected MemoryChunkPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker memoryChunkPoolStatsTracker) {
    super(memoryTrimmableRegistry, poolParams, memoryChunkPoolStatsTracker);
    SparseIntArray bucketSizes = poolParams.bucketSizes;
    mBucketSizes = new int[bucketSizes.size()];
    for (int i = 0; i < mBucketSizes.length; ++i) {
      mBucketSizes[i] = bucketSizes.keyAt(i);
    }
    initialize();
  }

  /** Gets the smallest buffer size */
  public int getMinBufferSize() {
    return mBucketSizes[0];
  }

  @Override
  public abstract MemoryChunk alloc(int bucketedSize);

  @Override
  public void free(MemoryChunk value) {
    Preconditions.checkNotNull(value);
    value.close();
  }

  @Override
  public int getSizeInBytes(int bucketedSize) {
    return bucketedSize;
  }

  @Override
  public int getBucketedSize(int requestSize) {
    if (requestSize <= 0) {
      throw new InvalidSizeException(requestSize);
    }

    // find the smallest bucketed size that is larger than the requested size
    for (int bucketedSize : mBucketSizes) {
      if (bucketedSize >= requestSize) {
        return bucketedSize;
      }
    }

    // requested size doesn't match our existing buckets - just return the requested size
    // this will eventually translate into a plain alloc/free paradigm
    return requestSize;
  }

  @Override
  public int getBucketedSizeForValue(MemoryChunk value) {
    Preconditions.checkNotNull(value);
    return value.getSize();
  }

  @Override
  public boolean isReusable(MemoryChunk value) {
    Preconditions.checkNotNull(value);
    return !value.isClosed();
  }
}
