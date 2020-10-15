/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.annotation.TargetApi;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.concurrent.ThreadSafe;

/** Manages a pool of ashmem memory chunks ({@link AshmemMemoryChunk}) */
@Nullsafe(Nullsafe.Mode.STRICT)
@ThreadSafe
@DoNotStrip
@TargetApi(27)
public class AshmemMemoryChunkPool extends MemoryChunkPool {

  @DoNotStrip
  public AshmemMemoryChunkPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker ashmemMemoryChunkPoolStatsTracker) {
    super(memoryTrimmableRegistry, poolParams, ashmemMemoryChunkPoolStatsTracker);
  }

  @Override
  public AshmemMemoryChunk alloc(int bucketedSize) {
    return new AshmemMemoryChunk(bucketedSize);
  }
}
