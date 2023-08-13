/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.util.SparseIntArray
import com.facebook.common.util.ByteConstants

/** Provides pool parameters ([PoolParams]) for [NativeMemoryChunkPool] */
object DefaultNativeMemoryChunkPoolParams {

  /**
   * Length of 'small' sized buckets. Bucket lengths for these buckets are larger because they're
   * smaller in size
   */
  private const val SMALL_BUCKET_LENGTH = 5

  /** Bucket lengths for 'large' (> 256KB) buckets */
  private const val LARGE_BUCKET_LENGTH = 2

  @JvmStatic
  fun get(): PoolParams {
    val DEFAULT_BUCKETS = SparseIntArray()
    DEFAULT_BUCKETS.put(1 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(2 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(4 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(8 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(16 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(32 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(64 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(128 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(256 * ByteConstants.KB, LARGE_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(512 * ByteConstants.KB, LARGE_BUCKET_LENGTH)
    DEFAULT_BUCKETS.put(1_024 * ByteConstants.KB, LARGE_BUCKET_LENGTH)
    return PoolParams(maxSizeSoftCap, maxSizeHardCap, DEFAULT_BUCKETS)
  }

  /**
   * [NativeMemoryChunkPool] manages memory on the native heap, so we don't need as strict caps as
   * we would if we were on the Dalvik heap. However, since native memory OOMs are significantly
   * more problematic than Dalvik OOMs, we would like to stay conservative.
   */
  private val maxSizeSoftCap: Int
    get() {
      val maxMemory = Math.min(Runtime.getRuntime().maxMemory(), Int.MAX_VALUE.toLong()).toInt()
      return if (maxMemory < 16 * ByteConstants.MB) {
        3 * ByteConstants.MB
      } else if (maxMemory < 32 * ByteConstants.MB) {
        6 * ByteConstants.MB
      } else {
        12 * ByteConstants.MB
      }
    }

  /**
   * We need a smaller cap for devices with less then 16 MB so that we don't run the risk of
   * evicting other processes from the native heap.
   */
  private val maxSizeHardCap: Int
    get() {
      val maxMemory = Math.min(Runtime.getRuntime().maxMemory(), Int.MAX_VALUE.toLong()).toInt()
      return if (maxMemory < 16 * ByteConstants.MB) {
        maxMemory / 2
      } else {
        maxMemory / 4 * 3
      }
    }
}
