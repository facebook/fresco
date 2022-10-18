/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import com.facebook.infer.annotation.ThreadSafe
import kotlin.jvm.JvmField

object BitmapCounterProvider {

  private const val KB: Long = 1_024
  private const val MB = 1_024 * KB

  /**
   * Our Bitmaps live in ashmem, meaning that they are pinned in Android's shared native memory.
   *
   * Therefore, we are not constrained by the max heap size of the dalvik heap, but we want to make
   * sure we don't use too much memory on low end devices, so that we don't force other background
   * process to be killed.
   */
  @JvmField val MAX_BITMAP_TOTAL_SIZE = maxSizeHardCap
  private var maxBitmapCount = BitmapCounterConfig.DEFAULT_MAX_BITMAP_COUNT

  @Volatile private var bitmapCounter: BitmapCounter? = null
  private val maxSizeHardCap: Int
    get() {
      val maxMemory = Math.min(Runtime.getRuntime().maxMemory(), Int.MAX_VALUE.toLong()).toInt()
      return if (maxMemory > 16 * MB) {
        maxMemory / 4 * 3
      } else {
        maxMemory / 2
      }
    }

  @JvmStatic
  fun initialize(bitmapCounterConfig: BitmapCounterConfig) {
    check(bitmapCounter == null) {
      "BitmapCounter has already been created! `BitmapCounterProvider.initialize(...)` should only be called before `BitmapCounterProvider.get()` or not at all!"
    }
    maxBitmapCount = bitmapCounterConfig.maxBitmapCount
  }

  @JvmStatic
  @ThreadSafe
  fun get(): BitmapCounter {
    if (bitmapCounter == null) {
      synchronized(BitmapCounterProvider::class.java) {
        if (bitmapCounter == null) {
          bitmapCounter = BitmapCounter(maxBitmapCount, MAX_BITMAP_TOTAL_SIZE)
        }
      }
    }
    return bitmapCounter!!
  }
}
