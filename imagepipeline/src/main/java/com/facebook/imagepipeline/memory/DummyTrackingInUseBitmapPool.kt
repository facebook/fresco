/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.graphics.Bitmap
import com.facebook.common.internal.Sets
import com.facebook.common.memory.MemoryTrimType
import com.facebook.imageutils.BitmapUtil

class DummyTrackingInUseBitmapPool : BitmapPool {

  /** An Identity hash-set to keep track of values by reference equality */
  private val inUseValues: MutableSet<Bitmap> = Sets.newIdentityHashSet()

  override fun trim(trimType: MemoryTrimType) {
    // nop
  }

  override fun get(size: Int): Bitmap {
    val result =
        Bitmap.createBitmap(
            1,
            Math.ceil(size / BitmapUtil.RGB_565_BYTES_PER_PIXEL.toDouble()).toInt(),
            Bitmap.Config.RGB_565)
    inUseValues.add(result)
    return result
  }

  override fun release(value: Bitmap) {
    checkNotNull(value)
    inUseValues.remove(value)
    value.recycle()
  }
}
