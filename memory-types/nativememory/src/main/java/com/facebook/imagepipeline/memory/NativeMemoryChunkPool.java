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

/** Manages a pool of native memory chunks ({@link NativeMemoryChunk}) */
@Nullsafe(Nullsafe.Mode.STRICT)
@ThreadSafe
@DoNotStrip
public class NativeMemoryChunkPool extends MemoryChunkPool {

  @DoNotStrip
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
