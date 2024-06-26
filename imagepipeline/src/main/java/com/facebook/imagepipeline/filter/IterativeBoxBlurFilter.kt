/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.graphics.Bitmap
import com.facebook.common.internal.Preconditions
import com.facebook.common.logging.FLog
import com.facebook.imageutils.BitmapUtil
import java.util.Locale

object IterativeBoxBlurFilter {

  private const val TAG = "IterativeBoxBlurFilter"

  /**
   * An in-place iterative box blur algorithm that runs faster than a traditional box blur.
   *
   * The individual box blurs are split up in vertical and horizontal direction. That allows us to
   * use a moving average implementation for blurring individual rows and columns.
   *
   * The runtime is: O(iterations * width * height) and therefore linear in the number of pixels
   *
   * The required memory is: 2 * radius * 256 * 4 Bytes + max(width, height) * 4 Bytes + width *
   * height * 4 Bytes (+constant)
   *
   * @param bitmap The [Bitmap] containing the image. The bitmap dimension need to be smaller than
   *   [BitmapUtil.MAX_BITMAP_DIMENSION]
   * @param iterations The number of iterations of the blurring algorithm > 0.
   * @param radius The radius of the blur with a supported range 0 < radius <= [ ]
   *   [RenderScriptBlurFilter.BLUR_MAX_RADIUS]
   */
  @JvmStatic
  fun boxBlurBitmapInPlace(bitmap: Bitmap, iterations: Int, radius: Int) {
    checkNotNull(bitmap)
    Preconditions.checkArgument(bitmap.isMutable)
    Preconditions.checkArgument(bitmap.height <= BitmapUtil.MAX_BITMAP_DIMENSION)
    Preconditions.checkArgument(bitmap.width <= BitmapUtil.MAX_BITMAP_DIMENSION)
    Preconditions.checkArgument(radius > 0 && radius <= RenderScriptBlurFilter.BLUR_MAX_RADIUS)
    Preconditions.checkArgument(iterations > 0)
    try {
      fastBoxBlur(bitmap, iterations, radius)
    } catch (oom: OutOfMemoryError) {
      FLog.e(
          TAG,
          String.format(
              null as Locale?,
              "OOM: %d iterations on %dx%d with %d radius",
              iterations,
              bitmap.width,
              bitmap.height,
              radius))
      throw oom
    }
  }

  private fun fastBoxBlur(bitmap: Bitmap, iterations: Int, radius: Int) {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    // The information written to an output pixels `x` are from `[x-radius, x+radius]` (inclusive)
    val diameter = radius + 1 + radius
    // Pre-compute division table: speed-up by factor 5(!)
    val div = IntArray(256 * diameter)

    // The following lines will fill-up at least the first `255 * diameter` entries with the mapping
    // `div[x] = (x + r) / d` (i.e. division of x by d rounded to the nearest number).
    var ptr = radius + 1
    for (b in 1..255) {
      for (d in 0 until diameter) {
        div[ptr] = b
        ptr++
      }
    }
    val tempRowOrColumn = IntArray(Math.max(w, h))
    for (i in 0 until iterations) {
      // Blur rows one-by-one
      for (row in 0 until h) {
        internalHorizontalBlur(pixels, tempRowOrColumn, w, row, diameter, div)
        System.arraycopy(tempRowOrColumn, 0, pixels, row * w, w)
      }

      // Blur columns one-by-one
      for (col in 0 until w) {
        internalVerticalBlur(pixels, tempRowOrColumn, w, h, col, diameter, div)
        var pos = col
        for (row in 0 until h) {
          pixels[pos] = tempRowOrColumn[row]
          pos += w
        }
      }
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
  }

  /**
   * Creates a blurred version of the given `row` of `pixel`. It uses a moving average algorithm
   * such that it reads every pixel of the row just once. The edge pixels are repeated to avoid
   * artifacts.
   *
   * Requires a pre-computed `div` table of size (255 * diameter) that maps x -> (x / diameter) (can
   * be rounded)
   */
  private fun internalHorizontalBlur(
      pixels: IntArray,
      outRow: IntArray,
      w: Int,
      row: Int,
      diameter: Int,
      div: IntArray
  ) {
    val firstInByte = w * row
    val lastInByte = w * (row + 1) - 1
    val radius = diameter shr 1
    var a = 0
    var r = 0
    var g = 0
    var b = 0
    var pixel: Int

    // Iterate over relative position to first pixel of row
    for (i in -radius until w + radius) {
      val ii = bound(firstInByte + i, firstInByte, lastInByte)
      pixel = pixels[ii]
      r += pixel shr 16 and 0xFF
      g += pixel shr 8 and 0xFF
      b += pixel and 0xFF
      a += pixel ushr 24
      if (i >= radius) {
        val outOffset = i - radius
        outRow[outOffset] = div[a] shl 24 or (div[r] shl 16) or (div[g] shl 8) or div[b]
        val j = i - (diameter - 1)
        val jj = bound(firstInByte + j, firstInByte, lastInByte)
        pixel = pixels[jj]
        r -= pixel shr 16 and 0xFF
        g -= pixel shr 8 and 0xFF
        b -= pixel and 0xFF
        a -= pixel ushr 24
      }
    }
  }

  /**
   * Creates a blurred version of the given `col` of `pixels`. It uses a moving average algorithm
   * such that it reads every pixel of the column just once. The edge pixels are repeated to avoid
   * artifacts.
   *
   * Requires a pre-computed `div` table of size (255 * diameter) that maps x -> (x / diameter) (can
   * be rounded)
   */
  private fun internalVerticalBlur(
      pixels: IntArray,
      outCol: IntArray,
      w: Int,
      h: Int,
      col: Int,
      diameter: Int,
      div: IntArray
  ) {
    val lastInByte = w * (h - 1) + col
    val radiusTimesW = (diameter shr 1) * w
    val diameterMinusOneTimesW = (diameter - 1) * w
    var a = 0
    var r = 0
    var g = 0
    var b = 0
    var pixel: Int
    var outColPos = 0

    // iterate over absolute positions in `pixelsIn`; `w` is the step width for moving down one row
    var i = col - radiusTimesW
    while (i <= lastInByte + radiusTimesW) {
      val ii = bound(i, col, lastInByte)
      pixel = pixels[ii]
      r += pixel shr 16 and 0xFF
      g += pixel shr 8 and 0xFF
      b += pixel and 0xFF
      a += pixel ushr 24
      val outPos = i - radiusTimesW
      if (outPos >= col) {
        outCol[outColPos] = div[a] shl 24 or (div[r] shl 16) or (div[g] shl 8) or div[b]
        outColPos++
        val j = i - diameterMinusOneTimesW
        val jj = bound(j, col, lastInByte)
        pixel = pixels[jj]
        r -= pixel shr 16 and 0xFF
        g -= pixel shr 8 and 0xFF
        b -= pixel and 0xFF
        a -= pixel ushr 24
      }
      i += w
    }
  }

  private fun bound(x: Int, l: Int, h: Int): Int {
    return if (x < l) l else if (x > h) h else x
  }
}
