/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import com.facebook.common.util.HashCodeUtil
import com.facebook.imageutils.BitmapUtil
import java.util.Locale
import kotlin.jvm.JvmField

/**
 * Options for resizing.
 *
 * Describes the target bounds for the image (width, height) in pixels, as well as the downscaling
 * policy to employ.
 */
class ResizeOptions
@JvmOverloads
constructor(
    /* target width (in pixels) */
    @JvmField val width: Int,
    /* target height (in pixels) */
    @JvmField val height: Int,
    /* max supported bitmap size (in pixels), defaults to BitmapUtil.MAX_BITMAP_DIMENSION */
    @JvmField val maxBitmapDimension: Float = BitmapUtil.MAX_BITMAP_DIMENSION,
    /* round-up fraction for resize process, defaults to DEFAULT_ROUNDUP_FRACTION */
    @JvmField val roundUpFraction: Float = DEFAULT_ROUNDUP_FRACTION
) {

  init {
    check(width > 0)
    check(height > 0)
  }

  override fun hashCode(): Int = HashCodeUtil.hashCode(width, height)

  override fun equals(other: Any?): Boolean {
    if (other === this) {
      return true
    }
    return other is ResizeOptions && width == other.width && height == other.height
  }

  override fun toString(): String = String.format(null as Locale?, "%dx%d", width, height)

  companion object {

    const val DEFAULT_ROUNDUP_FRACTION = 2.0f / 3

    /** @return new ResizeOptions, if the width and height values are valid, and null otherwise */
    @JvmStatic
    fun forDimensions(width: Int, height: Int): ResizeOptions? =
        if (width <= 0 || height <= 0) {
          null
        } else {
          ResizeOptions(width, height)
        }

    /** @return new ResizeOptions, if the width and height values are valid, and null otherwise */
    @JvmStatic
    fun forSquareSize(size: Int): ResizeOptions? =
        if (size <= 0) {
          null
        } else {
          ResizeOptions(size, size)
        }
  }
}
