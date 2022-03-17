/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.renderer.util

class ColorUtils {
  companion object {

    /**
     * Applies the given alpha to the color.
     *
     * @param color to add alpha
     * @param alpha to adjust opacity
     */
    fun multiplyColorAlpha(color: Int, alpha: Int): Int {
      return when (alpha) {
        255 -> color
        0 -> color and 0x00FFFFFF
        else -> {
          val cappedAlpha = alpha + (alpha shr 7) // make it 0..256
          val colorAlpha = color ushr 24
          val multipliedAlpha = colorAlpha * cappedAlpha shr 8
          multipliedAlpha shl 24 or (color and 0x00FFFFFF)
        }
      }
    }
  }
}
