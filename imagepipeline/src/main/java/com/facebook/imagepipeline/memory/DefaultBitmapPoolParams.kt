/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.util.SparseIntArray
import com.facebook.common.util.ByteConstants

/** Provides pool parameters for [BitmapPool] */
object DefaultBitmapPoolParams {

  /**
   * Our Bitmaps live in ashmem, meaning that they are pinned in androids' shared native memory.
   * Therefore, we are not constrained by the max heap size of the dalvik heap, but we want to make
   * sure we don't use too much memory on low end devices, so that we don't force other background
   * process to be evicted.
   */
  val maxSizeHardCap: Int
    get() {
      val maxMemory = Math.min(Runtime.getRuntime().maxMemory(), Int.MAX_VALUE.toLong()).toInt()
      return if (maxMemory > 16 * ByteConstants.MB) {
        maxMemory / 4 * 3
      } else {
        maxMemory / 2
      }
    }

  /** This will cause all get/release calls to behave like alloc/free calls i.e. no pooling. */
  private val DEFAULT_BUCKETS = SparseIntArray(0)

  @JvmStatic fun get(): PoolParams = PoolParams(DEFAULT_BUCKETS)
}
