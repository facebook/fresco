/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.graphics.Bitmap
import com.facebook.common.memory.MemoryTrimType
import com.facebook.imageutils.BitmapUtil

class DummyBitmapPool : BitmapPool {

  override fun trim(trimType: MemoryTrimType) {
    // nop
  }

  override fun get(size: Int): Bitmap =
      Bitmap.createBitmap(
          1,
          Math.ceil(size / BitmapUtil.RGB_565_BYTES_PER_PIXEL.toDouble()).toInt(),
          Bitmap.Config.RGB_565)

  override fun release(value: Bitmap) {
    checkNotNull(value)
    value.recycle()
  }
}
