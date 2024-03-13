/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat

import com.facebook.common.internal.ImmutableList

/** Default image formats that Fresco supports. */
object DefaultImageFormats {
  @JvmField val JPEG = ImageFormat("JPEG", "jpeg")
  @JvmField val PNG = ImageFormat("PNG", "png")
  @JvmField val GIF = ImageFormat("GIF", "gif")
  @JvmField val BMP = ImageFormat("BMP", "bmp")
  @JvmField val ICO = ImageFormat("ICO", "ico")
  @JvmField val WEBP_SIMPLE = ImageFormat("WEBP_SIMPLE", "webp")
  @JvmField val WEBP_LOSSLESS = ImageFormat("WEBP_LOSSLESS", "webp")
  @JvmField val WEBP_EXTENDED = ImageFormat("WEBP_EXTENDED", "webp")
  @JvmField val WEBP_EXTENDED_WITH_ALPHA = ImageFormat("WEBP_EXTENDED_WITH_ALPHA", "webp")
  @JvmField val WEBP_ANIMATED = ImageFormat("WEBP_ANIMATED", "webp")
  @JvmField val HEIF = ImageFormat("HEIF", "heif")
  @JvmField val DNG = ImageFormat("DNG", "dng")

  /**
   * Check if the given image format is a WebP image format (static or animated).
   *
   * @param imageFormat the image format to check
   * @return true if WebP format
   */
  @JvmStatic
  fun isWebpFormat(imageFormat: ImageFormat): Boolean {
    return isStaticWebpFormat(imageFormat) || imageFormat === DefaultImageFormats.WEBP_ANIMATED
  }

  /**
   * Check if the given image format is static WebP (not animated).
   *
   * @param imageFormat the image format to check
   * @return true if static WebP
   */
  @JvmStatic
  fun isStaticWebpFormat(imageFormat: ImageFormat): Boolean {
    return imageFormat === DefaultImageFormats.WEBP_SIMPLE ||
        imageFormat === DefaultImageFormats.WEBP_LOSSLESS ||
        imageFormat === DefaultImageFormats.WEBP_EXTENDED ||
        imageFormat === DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA
  }

  private var sAllDefaultFormats: ImmutableList<ImageFormat>? = null

  @JvmStatic
  val defaultFormats: List<ImageFormat>
    /**
     * Get all default formats supported by Fresco. Does not include [ImageFormat#UNKNOWN].
     *
     * @return all supported default formats
     */
    get() {
      if (sAllDefaultFormats == null) {
        val mDefaultFormats: MutableList<ImageFormat> = ArrayList(9)
        mDefaultFormats.add(DefaultImageFormats.JPEG)
        mDefaultFormats.add(DefaultImageFormats.PNG)
        mDefaultFormats.add(DefaultImageFormats.GIF)
        mDefaultFormats.add(DefaultImageFormats.BMP)
        mDefaultFormats.add(DefaultImageFormats.ICO)
        mDefaultFormats.add(DefaultImageFormats.WEBP_SIMPLE)
        mDefaultFormats.add(DefaultImageFormats.WEBP_LOSSLESS)
        mDefaultFormats.add(DefaultImageFormats.WEBP_EXTENDED)
        mDefaultFormats.add(DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA)
        mDefaultFormats.add(DefaultImageFormats.WEBP_ANIMATED)
        mDefaultFormats.add(DefaultImageFormats.HEIF)
        sAllDefaultFormats = ImmutableList.copyOf(mDefaultFormats)
      }
      return sAllDefaultFormats!!
    }
}
