/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import com.facebook.common.memory.MemoryTrimmableRegistry;
import javax.annotation.concurrent.ThreadSafe;

/** Manages a pool of native memory chunks ({@link NativeMemoryChunk}) */
@ThreadSafe
public class NativeMemoryChunkPool extends MemoryChunkPool {

  public NativeMemoryChunkPool(
      MemoryTrimmableRegistry memoryTrimmableRegistry,
      PoolParams poolParams,
      PoolStatsTracker nativeMemoryChunkPoolStatsTracker) {
    super(memoryTrimmableRegistry, poolParams, nativeMemoryChunkPoolStatsTracker);
  }

  @Override
  protected NativeMemoryChunk alloc(int bucketedSize) {
    return new NativeMemoryChunk(bucketedSize);
  }
}
