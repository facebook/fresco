/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing

import android.util.SparseIntArray
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.imagepipeline.memory.BufferMemoryChunk
import com.facebook.imagepipeline.memory.BufferMemoryChunkPool
import com.facebook.imagepipeline.memory.PoolParams
import com.facebook.imagepipeline.memory.PoolStatsTracker
import org.mockito.kotlin.mock

/** A 'fake' [com.facebook.imagepipeline.memory.BufferMemoryChunk] instance as a test helper */
class FakeBufferMemoryChunkPool
@JvmOverloads
constructor(poolParams: PoolParams = PoolParams(128, bucketSizes)) :
    BufferMemoryChunkPool(mock<MemoryTrimmableRegistry>(), poolParams, mock<PoolStatsTracker>()) {

  override fun alloc(bucketedSize: Int): BufferMemoryChunk = BufferMemoryChunk(bucketedSize)

  companion object {
    private val bucketSizes: SparseIntArray
      get() {
        val bucketSizes = SparseIntArray()
        bucketSizes.put(4, 10)
        bucketSizes.put(8, 10)
        bucketSizes.put(16, 10)
        bucketSizes.put(32, 10)
        return bucketSizes
      }
  }
}
