/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import android.graphics.Bitmap
import com.facebook.common.internal.DoNotStrip

/** A fast native rounding filter. */
@DoNotStrip
object NativeRoundingFilter {

  init {
    NativeFiltersLoader.load()
  }

  @JvmStatic
  fun toCircle(bitmap: Bitmap) {
    toCircle(bitmap, false)
  }

  @JvmStatic
  fun toCircleFast(bitmap: Bitmap) {
    toCircleFast(bitmap, false)
  }

  @JvmStatic
  fun addRoundedCorners(
      bitmap: Bitmap,
      radiusTopLeft: Int,
      radiusTopRight: Int,
      radiusBottomRight: Int,
      radiusBottomLeft: Int,
  ) {
    nativeAddRoundedCornersFilter(
        bitmap,
        radiusTopLeft,
        radiusTopRight,
        radiusBottomRight,
        radiusBottomLeft,
    )
  }

  /**
   * This is a fast, native implementation for rounding a bitmap. It takes the given bitmap and
   * modifies it to be circular.
   *
   * This implementation does not change the underlying bitmap dimensions, it only sets pixels that
   * are outside of the circle to a transparent color.
   *
   * @param bitmap the bitmap to modify
   */
  @JvmStatic
  /**
   * This is a fast, native implementation for rounding a bitmap. It takes the given bitmap and
   * modifies it to be circular.
   *
   * This implementation does not change the underlying bitmap dimensions, it only sets pixels that
   * are outside of the circle to a transparent color.
   *
   * @param bitmap the bitmap to modify
   */
  @DoNotStrip
  fun toCircle(bitmap: Bitmap, antiAliased: Boolean) {
    checkNotNull(bitmap)
    if (bitmap.width < 3 || bitmap.height < 3) {
      // Image too small to round
      return
    }
    nativeToCircleFilter(bitmap, antiAliased)
  }

  @JvmStatic
  @DoNotStrip
  fun toCircleFast(bitmap: Bitmap, antiAliased: Boolean) {
    checkNotNull(bitmap)
    if (bitmap.width < 3 || bitmap.height < 3) {
      // Image too small to round
      return
    }
    nativeToCircleFastFilter(bitmap, antiAliased)
  }

  @JvmStatic
  fun toCircleWithBorder(bitmap: Bitmap, colorARGB: Int, borderWidthPx: Int, antiAliased: Boolean) {
    checkNotNull(bitmap)
    if (bitmap.width < 3 || bitmap.height < 3) {
      // Image too small to round
      return
    }
    nativeToCircleWithBorderFilter(bitmap, colorARGB, borderWidthPx, antiAliased)
  }

  @JvmStatic
  @DoNotStrip
  private external fun nativeToCircleFilter(bitmap: Bitmap, antiAliased: Boolean)

  @JvmStatic
  @DoNotStrip
  private external fun nativeToCircleFastFilter(bitmap: Bitmap, antiAliased: Boolean)

  @JvmStatic
  @DoNotStrip
  private external fun nativeToCircleWithBorderFilter(
      bitmap: Bitmap,
      colorARGB: Int,
      borderWidthPx: Int,
      antiAliased: Boolean,
  )

  @JvmStatic
  @DoNotStrip
  private external fun nativeAddRoundedCornersFilter(
      bitmap: Bitmap,
      radiusTopLeft: Int,
      radiusTopRight: Int,
      radiusBottomRight: Int,
      radiusBottomLeft: Int,
  )
}
