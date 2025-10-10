/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import android.graphics.Bitmap
import com.facebook.common.internal.DoNotStrip

/**
 * Utility methods for handling Bitmaps.
 *
 * Native code used by this class is shipped as part of libimagepipeline.so
 */
@DoNotStrip
object Bitmaps {

  init {
    ImagePipelineNativeLoader.load()
  }

  /**
   * This blits the pixel data from src to dest.
   *
   * The destination bitmap must have both a height and a width equal to the source. For maximum
   * speed stride should be equal as well.
   *
   * Both bitmaps must use the same [android.graphics.Bitmap.Config] format.
   *
   * If the src is purgeable, it will be decoded as part of this operation if it was purged. The
   * dest should not be purgeable. If it is, the copy will still take place, but will be lost the
   * next time the dest gets purged, without warning.
   *
   * The dest must be mutable.
   *
   * @param dest Bitmap to copy into
   * @param src Bitmap to copy out of
   */
  @JvmStatic
  /**
   * This blits the pixel data from src to dest.
   *
   * The destination bitmap must have both a height and a width equal to the source. For maximum
   * speed stride should be equal as well.
   *
   * Both bitmaps must use the same [android.graphics.Bitmap.Config] format.
   *
   * If the src is purgeable, it will be decoded as part of this operation if it was purged. The
   * dest should not be purgeable. If it is, the copy will still take place, but will be lost the
   * next time the dest gets purged, without warning.
   *
   * The dest must be mutable.
   *
   * @param dest Bitmap to copy into
   * @param src Bitmap to copy out of
   */
  @DoNotStrip
  fun copyBitmap(dest: Bitmap, src: Bitmap) {
    require(src.config == dest.config)
    require(dest.isMutable)
    require(dest.width == src.width)
    require(dest.height == src.height)
    nativeCopyBitmap(dest, dest.rowBytes, src, src.rowBytes, dest.height)
  }

  @JvmStatic
  @DoNotStrip
  private external fun nativeCopyBitmap(
      dest: Bitmap,
      destStride: Int,
      src: Bitmap,
      srcStride: Int,
      rows: Int,
  )
}
