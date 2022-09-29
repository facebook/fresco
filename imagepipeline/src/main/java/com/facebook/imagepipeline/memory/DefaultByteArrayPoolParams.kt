/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.util.SparseIntArray
import com.facebook.common.util.ByteConstants

/** Provides pool parameters ([PoolParams]) for common [ByteArrayPool] */
object DefaultByteArrayPoolParams {

  private const val DEFAULT_IO_BUFFER_SIZE = 16 * ByteConstants.KB

  /*
   * There are up to 5 simultaneous IO operations in new pipeline performed by:
   * - 3 image-fetch threads
   * - 2 image-cache threads
   * We should be able to satisfy these requirements without any allocations
   */
  private const val DEFAULT_BUCKET_SIZE = 5
  private const val MAX_SIZE_SOFT_CAP = 5 * DEFAULT_IO_BUFFER_SIZE

  /** We don't need hard cap here. */
  private const val MAX_SIZE_HARD_CAP = 1 * ByteConstants.MB

  /** Get default [PoolParams]. */
  @JvmStatic
  fun get(): PoolParams {
    // This pool supports only one bucket size: DEFAULT_IO_BUFFER_SIZE
    val defaultBuckets = SparseIntArray()
    defaultBuckets.put(DEFAULT_IO_BUFFER_SIZE, DEFAULT_BUCKET_SIZE)
    return PoolParams(MAX_SIZE_SOFT_CAP, MAX_SIZE_HARD_CAP, defaultBuckets)
  }
}
