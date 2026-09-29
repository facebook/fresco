/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import com.facebook.common.internal.DoNotStrip
import com.facebook.common.memory.MemoryTrimmableRegistry
import javax.annotation.concurrent.ThreadSafe

/** Manages a pool of native memory chunks ([NativeMemoryChunk]) */
@ThreadSafe
@DoNotStrip
open class NativeMemoryChunkPool
@DoNotStrip
constructor(
    memoryTrimmableRegistry: MemoryTrimmableRegistry,
    poolParams: PoolParams,
    nativeMemoryChunkPoolStatsTracker: PoolStatsTracker,
) : MemoryChunkPool(memoryTrimmableRegistry, poolParams, nativeMemoryChunkPoolStatsTracker) {
  override fun alloc(bucketedSize: Int): NativeMemoryChunk = NativeMemoryChunk(bucketedSize)
}
