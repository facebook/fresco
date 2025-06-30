/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.Preconditions
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.infer.annotation.Nullsafe
import javax.annotation.concurrent.ThreadSafe

/**
 * A special byte-array pool designed to minimize allocations.
 *
 * The length of each bucket's free list is capped at the number of threads using the pool.
 *
 * The free list of each bucket uses [OOMSoftReference]s.
 */
@ThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
class FlexByteArrayPool(memoryTrimmableRegistry: MemoryTrimmableRegistry?, params: PoolParams) {
  private val mResourceReleaser: ResourceReleaser<ByteArray>
  @VisibleForTesting val mDelegatePool: SoftRefByteArrayPool

  init {
    Preconditions.checkArgument(params.maxNumThreads > 0)
    mDelegatePool =
        SoftRefByteArrayPool(memoryTrimmableRegistry, params, NoOpPoolStatsTracker.getInstance())
    mResourceReleaser = ResourceReleaser { unused -> this@FlexByteArrayPool.release(unused) }
  }

  operator fun get(size: Int): CloseableReference<ByteArray> {
    return CloseableReference.of(mDelegatePool[size], mResourceReleaser)
  }

  fun release(value: ByteArray) {
    mDelegatePool.release(value)
  }

  val stats: Map<String, Int>
    get() = mDelegatePool.stats

  val minBufferSize: Int
    get() = mDelegatePool.minBufferSize

  @VisibleForTesting
  class SoftRefByteArrayPool(
      memoryTrimmableRegistry: MemoryTrimmableRegistry?,
      poolParams: PoolParams?,
      poolStatsTracker: PoolStatsTracker?
  ) : GenericByteArrayPool(memoryTrimmableRegistry!!, poolParams!!, poolStatsTracker!!) {
    override fun newBucket(bucketedSize: Int): Bucket<ByteArray> {
      return OOMSoftReferenceBucket(getSizeInBytes(bucketedSize), poolParams.maxNumThreads, 0)
    }
  }
}
