/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.graphics.Bitmap
import com.facebook.common.internal.Preconditions
import com.facebook.imageutils.BitmapUtil

/**
 * Modified midpoint circle algorithm. Clients that look for better performances should use the
 * native implementation of this algorithm in NativeRoundingFilter.
 */
object InPlaceRoundFilter {

  /**
   * An implementation for rounding a given bitmap to a circular shape. The underlying
   * implementation uses a modified midpoint circle algorithm but instead of drawing a circle, it
   * clears all pixels starting from the circle all the way to the bitmap edges.
   *
   * @param bitmap The input [Bitmap]
   */
  @JvmStatic
  fun roundBitmapInPlace(bitmap: Bitmap) {
    checkNotNull(bitmap)
    val w = bitmap.width
    val h = bitmap.height
    val radius = Math.min(w, h) / 2
    val centerX = w / 2
    val centerY = h / 2
    // Nothing to do if the radius is equal to 0.
    if (radius == 0) {
      return
    }
    Preconditions.checkArgument(radius >= 1)
    Preconditions.checkArgument(w > 0 && w <= BitmapUtil.MAX_BITMAP_DIMENSION)
    Preconditions.checkArgument(h > 0 && h <= BitmapUtil.MAX_BITMAP_DIMENSION)
    Preconditions.checkArgument(centerX > 0 && centerX < w)
    Preconditions.checkArgument(centerY > 0 && centerY < h)
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    var x = radius - 1
    var y = 0
    val maxX = centerX + x
    val maxY = centerY + x
    val minX = centerX - x
    val minY = centerY - x
    Preconditions.checkArgument(minX >= 0 && minY >= 0 && maxX < w && maxY < h)
    var dx = 1
    var dy = 1
    val rInc = -radius * 2
    val transparentColor = IntArray(w)
    var err = dx + rInc
    var cXpX: Int
    var cXmX: Int
    var cXpY: Int
    var cXmY: Int
    var cYpX: Int
    var cYmX: Int
    var cYpY: Int
    var cYmY: Int
    var offA: Int
    var offB: Int
    var offC: Int
    var offD: Int
    while (x >= y) {
      cXpX = centerX + x
      cXmX = centerX - x
      cXpY = centerX + y
      cXmY = centerX - y
      cYpX = centerY + x
      cYmX = centerY - x
      cYpY = centerY + y
      cYmY = centerY - y
      Preconditions.checkArgument(x >= 0 && cXpY < w && cXmY >= 0 && cYpY < h && cYmY >= 0)
      offA = w * cYpY
      offB = w * cYmY
      offC = w * cYpX
      offD = w * cYmX

      // Clear left
      System.arraycopy(transparentColor, 0, pixels, offA, cXmX)
      System.arraycopy(transparentColor, 0, pixels, offB, cXmX)
      System.arraycopy(transparentColor, 0, pixels, offC, cXmY)
      System.arraycopy(transparentColor, 0, pixels, offD, cXmY)

      // Clear right
      System.arraycopy(transparentColor, 0, pixels, offA + cXpX, w - cXpX)
      System.arraycopy(transparentColor, 0, pixels, offB + cXpX, w - cXpX)
      System.arraycopy(transparentColor, 0, pixels, offC + cXpY, w - cXpY)
      System.arraycopy(transparentColor, 0, pixels, offD + cXpY, w - cXpY)
      if (err <= 0) {
        y++
        dy += 2
        err += dy
      }
      if (err > 0) {
        x--
        dx += 2
        err += dx + rInc
      }
    }

    // Clear top / bottom if height > width
    for (i in centerY - radius downTo 0) {
      System.arraycopy(transparentColor, 0, pixels, i * w, w)
    }
    for (i in centerY + radius until h) {
      System.arraycopy(transparentColor, 0, pixels, i * w, w)
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
  }
}
