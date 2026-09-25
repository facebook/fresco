/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.webp

import java.io.UnsupportedEncodingException

object WebpSupportStatus {

  const val sIsSimpleWebpSupported: Boolean = true

  private const val isExtendedWebpSupported: Boolean = true

  const val sIsExtendedWebpSupported: Boolean = isExtendedWebpSupported

  @JvmField var sWebpBitmapFactory: WebpBitmapFactory? = null

  private var webpLibraryChecked = false

  @JvmStatic
  fun loadWebpBitmapFactoryIfExists(): WebpBitmapFactory? {
    if (webpLibraryChecked) {
      return sWebpBitmapFactory
    }
    var loadedWebpBitmapFactory: WebpBitmapFactory? = null
    try {
      loadedWebpBitmapFactory =
          Class.forName("com.facebook.webpsupport.WebpBitmapFactoryImpl").newInstance()
              as WebpBitmapFactory
    } catch (e: Throwable) {
      // Head in the sand
    }
    webpLibraryChecked = true
    return loadedWebpBitmapFactory
  }

  /**
   * Helper method that transforms provided string into its byte representation using ASCII encoding
   *
   * @param value bytes value
   * @return byte array representing ascii encoded value
   */
  private fun asciiBytes(value: String): ByteArray {
    try {
      return value.toByteArray(charset("ASCII"))
    } catch (uee: UnsupportedEncodingException) {
      // won't happen
      throw RuntimeException("ASCII not found!", uee)
    }
  }

  /**
   * Each WebP header should consist of at least 20 bytes and start with "RIFF" bytes followed by
   * some 4 bytes and "WEBP" bytes. A more detailed description if WebP can be found here:
   * [ https://developers.google.com/speed/webp/docs/riff_container](https://developers.google.com/speed/webp/docs/riff_container)
   */
  private const val SIMPLE_WEBP_HEADER_LENGTH = 20

  /** Each VP8X WebP image has a "features" byte following its ChunkHeader('VP8X') */
  private const val EXTENDED_WEBP_HEADER_LENGTH = 21

  private val WEBP_RIFF_BYTES = asciiBytes("RIFF")
  private val WEBP_NAME_BYTES = asciiBytes("WEBP")

  /** This is a constant used to detect different WebP's formats: vp8, vp8l and vp8x. */
  private val WEBP_VP8_BYTES = asciiBytes("VP8 ")

  private val WEBP_VP8L_BYTES = asciiBytes("VP8L")
  private val WEBP_VP8X_BYTES = asciiBytes("VP8X")

  @JvmStatic
  fun isWebpSupportedByPlatform(
      imageHeaderBytes: ByteArray,
      offset: Int,
      headerSize: Int,
  ): Boolean {
    if (isSimpleWebpHeader(imageHeaderBytes, offset)) {
      return sIsSimpleWebpSupported
    }

    if (isLosslessWebpHeader(imageHeaderBytes, offset)) {
      return sIsExtendedWebpSupported
    }

    if (isExtendedWebpHeader(imageHeaderBytes, offset, headerSize)) {
      if (isAnimatedWebpHeader(imageHeaderBytes, offset)) {
        return false
      }
      return sIsExtendedWebpSupported
    }

    return false
  }

  @JvmStatic
  fun isAnimatedWebpHeader(imageHeaderBytes: ByteArray, offset: Int): Boolean {
    val isVp8x = matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8X_BYTES)
    // ANIM is 2nd bit (00000010 == 2) on 21st byte (imageHeaderBytes[20])
    val hasAnimationBit = (imageHeaderBytes[offset + 20].toInt() and 2) == 2
    return isVp8x && hasAnimationBit
  }

  @JvmStatic
  fun isSimpleWebpHeader(imageHeaderBytes: ByteArray, offset: Int): Boolean =
      matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8_BYTES)

  @JvmStatic
  fun isLosslessWebpHeader(imageHeaderBytes: ByteArray, offset: Int): Boolean =
      matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8L_BYTES)

  @JvmStatic
  fun isExtendedWebpHeader(
      imageHeaderBytes: ByteArray,
      offset: Int,
      headerSize: Int,
  ): Boolean =
      headerSize >= EXTENDED_WEBP_HEADER_LENGTH &&
          matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8X_BYTES)

  @JvmStatic
  fun isExtendedWebpHeaderWithAlpha(
      imageHeaderBytes: ByteArray,
      offset: Int,
  ): Boolean {
    val isVp8x = matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8X_BYTES)
    // Has ALPHA is 5th bit (00010000 == 16) on 21st byte (imageHeaderBytes[20])
    val hasAlphaBit = (imageHeaderBytes[offset + 20].toInt() and 16) == 16
    return isVp8x && hasAlphaBit
  }

  /**
   * Checks if imageHeaderBytes contains WEBP_RIFF_BYTES and WEBP_NAME_BYTES and if the header is
   * long enough to be WebP's header. WebP file format can be found here:
   * [ https://developers.google.com/speed/webp/docs/riff_container](https://developers.google.com/speed/webp/docs/riff_container)
   *
   * @param imageHeaderBytes image header bytes
   * @return true if imageHeaderBytes contains a valid webp header
   */
  @JvmStatic
  fun isWebpHeader(
      imageHeaderBytes: ByteArray,
      offset: Int,
      headerSize: Int,
  ): Boolean =
      headerSize >= SIMPLE_WEBP_HEADER_LENGTH &&
          matchBytePattern(
              imageHeaderBytes,
              offset,
              WEBP_RIFF_BYTES,
          ) &&
          matchBytePattern(imageHeaderBytes, offset + 8, WEBP_NAME_BYTES)

  private fun matchBytePattern(
      byteArray: ByteArray?,
      offset: Int,
      pattern: ByteArray?,
  ): Boolean {
    if (pattern == null || byteArray == null) {
      return false
    }
    if (pattern.size + offset > byteArray.size) {
      return false
    }

    return pattern.indices.none { i -> byteArray[i + offset] != pattern[i] }
  }
}
