/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.annotation.TargetApi
import com.facebook.common.internal.DoNotStrip
import com.facebook.common.memory.MemoryTrimmableRegistry
import javax.annotation.concurrent.ThreadSafe

/** Manages a pool of ashmem memory chunks ([AshmemMemoryChunk]) */
@ThreadSafe
@DoNotStrip
@TargetApi(27)
open class AshmemMemoryChunkPool
@DoNotStrip
constructor(
    memoryTrimmableRegistry: MemoryTrimmableRegistry,
    poolParams: PoolParams,
    ashmemMemoryChunkPoolStatsTracker: PoolStatsTracker,
) : MemoryChunkPool(memoryTrimmableRegistry, poolParams, ashmemMemoryChunkPoolStatsTracker) {
  public override fun alloc(bucketedSize: Int): AshmemMemoryChunk = AshmemMemoryChunk(bucketedSize)
}
