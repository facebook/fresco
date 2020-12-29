/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.concurrent.ThreadSafe;

/** Manages a pool of buffer memory chunks ({@link BufferMemoryChunk}) */
@Nullsafe(Nullsafe.Mode.STRICT)
@ThreadSafe
@DoNotStrip
public class BufferMemoryChunkPool extends MemoryChunkPool {

  @DoNotStrip
  public BufferMemoryChunkPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker bufferMemoryChunkPoolStatsTracker) {
    super(memoryTrimmableRegistry, poolParams, bufferMemoryChunkPoolStatsTracker);
  }

  @Override
  protected BufferMemoryChunk alloc(int bucketedSize) {
    return new BufferMemoryChunk(bucketedSize);
  }
}
