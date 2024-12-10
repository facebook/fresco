/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat

/** Default image formats that Fresco supports. */
object DefaultImageFormats {
  @JvmField val JPEG: ImageFormat = ImageFormat("JPEG", "jpeg")
  @JvmField val PNG: ImageFormat = ImageFormat("PNG", "png")
  @JvmField val GIF: ImageFormat = ImageFormat("GIF", "gif")
  @JvmField val BMP: ImageFormat = ImageFormat("BMP", "bmp")
  @JvmField val ICO: ImageFormat = ImageFormat("ICO", "ico")
  @JvmField val WEBP_SIMPLE: ImageFormat = ImageFormat("WEBP_SIMPLE", "webp")
  @JvmField val WEBP_LOSSLESS: ImageFormat = ImageFormat("WEBP_LOSSLESS", "webp")
  @JvmField val WEBP_EXTENDED: ImageFormat = ImageFormat("WEBP_EXTENDED", "webp")
  @JvmField
  val WEBP_EXTENDED_WITH_ALPHA: ImageFormat = ImageFormat("WEBP_EXTENDED_WITH_ALPHA", "webp")
  @JvmField val WEBP_ANIMATED: ImageFormat = ImageFormat("WEBP_ANIMATED", "webp")
  @JvmField val HEIF: ImageFormat = ImageFormat("HEIF", "heif")
  @JvmField val DNG: ImageFormat = ImageFormat("DNG", "dng")
  @JvmField val BINARY_XML: ImageFormat = ImageFormat("BINARY_XML", "xml")
  @JvmField val AVIF: ImageFormat = ImageFormat("AVIF", "avif")

  /**
   * Check if the given image format is a WebP image format (static or animated).
   *
   * @param imageFormat the image format to check
   * @return true if WebP format
   */
  @JvmStatic
  fun isWebpFormat(imageFormat: ImageFormat): Boolean {
    return isStaticWebpFormat(imageFormat) || imageFormat === WEBP_ANIMATED
  }

  /**
   * Check if the given image format is static WebP (not animated).
   *
   * @param imageFormat the image format to check
   * @return true if static WebP
   */
  @JvmStatic
  fun isStaticWebpFormat(imageFormat: ImageFormat): Boolean {
    return imageFormat === WEBP_SIMPLE ||
        imageFormat === WEBP_LOSSLESS ||
        imageFormat === WEBP_EXTENDED ||
        imageFormat === WEBP_EXTENDED_WITH_ALPHA
  }

  /**
   * Get all default formats supported by Fresco. Does not include [ImageFormat#UNKNOWN].
   *
   * @return all supported default formats
   */
  @JvmField
  val defaultFormats: List<ImageFormat> =
      listOf(
          JPEG,
          PNG,
          GIF,
          BMP,
          ICO,
          WEBP_SIMPLE,
          WEBP_LOSSLESS,
          WEBP_EXTENDED,
          WEBP_EXTENDED_WITH_ALPHA,
          WEBP_ANIMATED,
          HEIF,
          BINARY_XML,
          AVIF,
      )
}
