/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import androidx.annotation.ColorInt

@Suppress("KtDataClass")
data class BorderOptions(
    @JvmField @field:ColorInt @param:ColorInt val color: Int,
    @JvmField val width: Float,
    @JvmField val padding: Float = 0f,
    @JvmField val scaleDownInsideBorders: Boolean = false
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    val otherOptions: BorderOptions = other as BorderOptions

    return color == otherOptions.color &&
        width == otherOptions.width &&
        padding == otherOptions.padding &&
        scaleDownInsideBorders == otherOptions.scaleDownInsideBorders
  }

  override fun hashCode(): Int {
    var result = color
    result = 31 * result + width.hashCode()
    result = 31 * result + padding.hashCode()
    result = 31 * result + scaleDownInsideBorders.hashCode()
    return result
  }

  companion object {
    /**
     * Create border options with padding and scaleDownInsideBorders. Note that currently padding is
     * not supported with RoundingOptions.asCircle().
     *
     * @param color The color of the border
     * @param width The width of the border, in pixels
     * @param padding Padding between the border and the image, in pixels
     * @param scaleDownInsideBorders true if scaled down inside border, false otherwise
     * @return BorderOptions
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        @ColorInt color: Int,
        width: Float,
        padding: Float = 0f,
        scaleDownInsideBorders: Boolean = false
    ): BorderOptions {
      return BorderOptions(color, width, padding, scaleDownInsideBorders)
    }
  }
}
