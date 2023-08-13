/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.util.SparseIntArray
import com.facebook.common.util.ByteConstants

/** Provides pool parameters ([PoolParams]) for [SharedByteArray] */
object DefaultFlexByteArrayPoolParams {

  // the default max buffer size we'll use
  const val DEFAULT_MAX_BYTE_ARRAY_SIZE = 4 * ByteConstants.MB

  // the min buffer size we'll use
  private const val DEFAULT_MIN_BYTE_ARRAY_SIZE = 128 * ByteConstants.KB

  // the maximum number of threads permitted to touch this pool
  val DEFAULT_MAX_NUM_THREADS = Runtime.getRuntime().availableProcessors()

  @JvmStatic
  fun generateBuckets(min: Int, max: Int, numThreads: Int): SparseIntArray {
    val buckets = SparseIntArray()
    var i = min
    while (i <= max) {
      buckets.put(i, numThreads)
      i *= 2
    }
    return buckets
  }

  @JvmStatic
  fun get(): PoolParams =
      PoolParams(
          /* maxSizeSoftCap */
          DEFAULT_MAX_BYTE_ARRAY_SIZE, /* maxSizeHardCap */
          DEFAULT_MAX_NUM_THREADS * DEFAULT_MAX_BYTE_ARRAY_SIZE, /* bucketSizes */
          generateBuckets(
              DEFAULT_MIN_BYTE_ARRAY_SIZE,
              DEFAULT_MAX_BYTE_ARRAY_SIZE,
              DEFAULT_MAX_NUM_THREADS), /* minBucketSize */
          DEFAULT_MIN_BYTE_ARRAY_SIZE, /* maxBucketSize */
          DEFAULT_MAX_BYTE_ARRAY_SIZE, /* maxNumThreads */
          DEFAULT_MAX_NUM_THREADS)
}
