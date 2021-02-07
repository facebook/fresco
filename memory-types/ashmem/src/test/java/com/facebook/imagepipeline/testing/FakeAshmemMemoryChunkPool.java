/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import android.util.SparseIntArray;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.imagepipeline.memory.AshmemMemoryChunk;
import com.facebook.imagepipeline.memory.AshmemMemoryChunkPool;
import com.facebook.imagepipeline.memory.PoolParams;
import com.facebook.imagepipeline.memory.PoolStatsTracker;
import org.mockito.Mockito;

/** A 'fake' {@link com.facebook.memory.ashmem.AshmemMemoryChunk} instance as a test helper */
public class FakeAshmemMemoryChunkPool extends AshmemMemoryChunkPool {

  public FakeAshmemMemoryChunkPool() {
    this(new PoolParams(128, getBucketSizes()));
  }

  public FakeAshmemMemoryChunkPool(PoolParams poolParams) {
    super(
        Mockito.mock(MemoryTrimmableRegistry.class),
        poolParams,
        Mockito.mock(PoolStatsTracker.class));
  }

  @Override
  public AshmemMemoryChunk alloc(int bucketedSize) {
    return new FakeAshmemMemoryChunk(bucketedSize);
  }

  private static SparseIntArray getBucketSizes() {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(4, 10);
    bucketSizes.put(8, 10);
    bucketSizes.put(16, 10);
    bucketSizes.put(32, 10);
    return bucketSizes;
  }
}
