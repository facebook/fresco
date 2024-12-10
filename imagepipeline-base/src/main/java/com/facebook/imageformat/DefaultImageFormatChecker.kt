/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat

import com.facebook.common.webp.WebpSupportStatus
import com.facebook.imageformat.ImageFormat.FormatChecker

/** Default image format checker that is able to determine all [DefaultImageFormats]. */
class DefaultImageFormatChecker : FormatChecker {

  /**
   * Maximum header size for any image type.
   *
   * This determines how much data [reads][ImageFormatChecker.getImageFormat]
   */
  override val headerSize: Int =
      checkNotNull(
          arrayOf(
                  EXTENDED_WEBP_HEADER_LENGTH,
                  SIMPLE_WEBP_HEADER_LENGTH,
                  JPEG_HEADER_LENGTH,
                  PNG_HEADER_LENGTH,
                  GIF_HEADER_LENGTH,
                  BMP_HEADER_LENGTH,
                  ICO_HEADER_LENGTH,
                  HEIF_HEADER_LENGTH,
                  BINARY_XML_HEADER_LENGTH,
                  AVIF_HEADER_LENGTH,
              )
              .maxOrNull())

  /**
   * Tries to match imageHeaderByte and headerSize against every known image format. If any match
   * succeeds, corresponding ImageFormat is returned.
   *
   * @param headerBytes the header bytes to check
   * @param headerSize the available header size
   * @return ImageFormat for given imageHeaderBytes or UNKNOWN if no such type could be recognized
   */
  override fun determineFormat(headerBytes: ByteArray, headerSize: Int): ImageFormat {
    if (WebpSupportStatus.isWebpHeader(headerBytes, 0, headerSize)) {
      return getWebpFormat(headerBytes, headerSize)
    }
    if (isJpegHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.JPEG
    }
    if (isPngHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.PNG
    }
    if (isGifHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.GIF
    }
    if (isBmpHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.BMP
    }
    if (isIcoHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.ICO
    }
    if (isAvifHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.AVIF
    }
    if (isHeifHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.HEIF
    }
    if (isBinaryXmlHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.BINARY_XML
    }
    return if (isDngHeader(headerBytes, headerSize)) {
      DefaultImageFormats.DNG
    } else {
      ImageFormat.Companion.UNKNOWN
    }
  }

  companion object {
    /**
     * Each WebP header should consist of at least 20 bytes and start with "RIFF" bytes followed by
     * some 4 bytes and "WEBP" bytes. More detailed description if WebP can be found here:
     * [ https://developers.google.com/speed/webp/docs/riff_container](https://developers.google.com/speed/webp/docs/riff_container)
     */
    private const val SIMPLE_WEBP_HEADER_LENGTH = 20
    /** Each VP8X WebP image has "features" byte following its ChunkHeader('VP8X') */
    private const val EXTENDED_WEBP_HEADER_LENGTH = 21

    /** Determines type of WebP image. imageHeaderBytes has to be header of a WebP image */
    private fun getWebpFormat(imageHeaderBytes: ByteArray, headerSize: Int): ImageFormat {
      check(WebpSupportStatus.isWebpHeader(imageHeaderBytes, 0, headerSize))
      if (WebpSupportStatus.isSimpleWebpHeader(imageHeaderBytes, 0)) {
        return DefaultImageFormats.WEBP_SIMPLE
      }
      if (WebpSupportStatus.isLosslessWebpHeader(imageHeaderBytes, 0)) {
        return DefaultImageFormats.WEBP_LOSSLESS
      }
      if (WebpSupportStatus.isExtendedWebpHeader(imageHeaderBytes, 0, headerSize)) {
        if (WebpSupportStatus.isAnimatedWebpHeader(imageHeaderBytes, 0)) {
          return DefaultImageFormats.WEBP_ANIMATED
        }
        return if (WebpSupportStatus.isExtendedWebpHeaderWithAlpha(imageHeaderBytes, 0)) {
          DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA
        } else {
          DefaultImageFormats.WEBP_EXTENDED
        }
      }
      return ImageFormat.Companion.UNKNOWN
    }

    /**
     * Every JPEG image should start with SOI mark (0xFF, 0xD8) followed by beginning of another
     * segment (0xFF)
     */
    private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val JPEG_HEADER_LENGTH = JPEG_HEADER.size

    /**
     * Checks if imageHeaderBytes starts with SOI (start of image) marker, followed by 0xFF. If
     * headerSize is lower than 3 false is returned. Description of jpeg format can be found here:
     * [ http://www.w3.org/Graphics/JPEG/itu-t81.pdf](http://www.w3.org/Graphics/JPEG/itu-t81.pdf)
     * Annex B deals with compressed data format
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes starts with SOI_BYTES and headerSize >= 3
     */
    private fun isJpegHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean =
        headerSize >= JPEG_HEADER.size &&
            ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, JPEG_HEADER)

    /** Every PNG image starts with 8 byte signature consisting of following bytes */
    private val PNG_HEADER =
        byteArrayOf(
            0x89.toByte(),
            'P'.code.toByte(),
            'N'.code.toByte(),
            'G'.code.toByte(),
            0x0D.toByte(),
            0x0A.toByte(),
            0x1A.toByte(),
            0x0A.toByte())
    private val PNG_HEADER_LENGTH = PNG_HEADER.size

    /**
     * Checks if array consisting of first headerSize bytes of imageHeaderBytes starts with png
     * signature. More information on PNG can be found there:
     * [ http://en.wikipedia.org/wiki/Portable_Network_Graphics](http://en.wikipedia.org/wiki/Portable_Network_Graphics)
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes starts with PNG_HEADER
     */
    private fun isPngHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean =
        headerSize >= PNG_HEADER.size &&
            ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, PNG_HEADER)

    /**
     * Every gif image starts with "GIF" bytes followed by bytes indicating version of gif standard
     */
    private val GIF_HEADER_87A: ByteArray = ImageFormatCheckerUtils.asciiBytes("GIF87a")
    private val GIF_HEADER_89A: ByteArray = ImageFormatCheckerUtils.asciiBytes("GIF89a")
    private const val GIF_HEADER_LENGTH = 6

    /**
     * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a gif
     * image. Details on GIF header can be found
     * [on page 7](http://www.w3.org/Graphics/GIF/spec-gif89a.txt)
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes is a valid header for a gif image
     */
    private fun isGifHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean {
      if (headerSize < GIF_HEADER_LENGTH) {
        return false
      }
      return ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, GIF_HEADER_87A) ||
          ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, GIF_HEADER_89A)
    }

    /** Every bmp image starts with "BM" bytes */
    private val BMP_HEADER: ByteArray = ImageFormatCheckerUtils.asciiBytes("BM")
    private val BMP_HEADER_LENGTH = BMP_HEADER.size

    /**
     * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a bmp
     * image. Details on BMP header can be found [
     * ](http://www.onicos.com/staff/iz/formats/bmp.html) *
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes is a valid header for a bmp image
     */
    private fun isBmpHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean =
        if (headerSize < BMP_HEADER.size) {
          false
        } else {
          ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, BMP_HEADER)
        }

    /** Every ico image starts with 0x00000100 bytes */
    private val ICO_HEADER = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte())
    private val ICO_HEADER_LENGTH = ICO_HEADER.size

    /**
     * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a ico
     * image. Details on ICO header can be found [
     * ](https://en.wikipedia.org/wiki/ICO_(file_format)) *
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes is a valid header for a ico image
     */
    private fun isIcoHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean =
        if (headerSize < ICO_HEADER.size) {
          false
        } else {
          ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, ICO_HEADER)
        }

    /**
     * Every HEIF image starts with a specific signature. It's guaranteed to include "ftyp". The 4th
     * byte of the header gives us the size of the box, then we have "ftyp" followed by the exact
     * image format which can be one of: heic, heix, hevc, hevx.
     */
    private val HEIF_HEADER_PREFIX: ByteArray = ImageFormatCheckerUtils.asciiBytes("ftyp")
    private val HEIF_HEADER_SUFFIXES =
        arrayOf<ByteArray>(
            ImageFormatCheckerUtils.asciiBytes("heic"),
            ImageFormatCheckerUtils.asciiBytes("heix"),
            ImageFormatCheckerUtils.asciiBytes("hevc"),
            ImageFormatCheckerUtils.asciiBytes("hevx"),
            ImageFormatCheckerUtils.asciiBytes("mif1"),
            ImageFormatCheckerUtils.asciiBytes("msf1"))
    private const val HEIF_HEADER_LENGTH = 12

    /**
     * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a HEIF
     * image. Details on HEIF header can be found at:
     * [](http://nokiatech.github.io/heif/technical.html)
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes is a valid header for a HEIF image
     */
    private fun isHeifHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean {
      if (headerSize < HEIF_HEADER_LENGTH) {
        return false
      }
      val boxLength = imageHeaderBytes[3]
      if (boxLength < 8) {
        return false
      }
      if (!ImageFormatCheckerUtils.hasPatternAt(imageHeaderBytes, HEIF_HEADER_PREFIX, 4)) {
        return false
      }
      return HEIF_HEADER_SUFFIXES.any { heifFtype ->
        ImageFormatCheckerUtils.hasPatternAt(imageHeaderBytes, heifFtype, 8)
      }
    }

    /**
     * DNG has TIFF format which begins with binary order ('II' or 'MM' (stands for intel and
     * motorola binary order respectively)) then a word containing the number 42
     * http://www.fileformat.info/format/tiff/corion.htm
     */
    private val DNG_HEADER_II =
        byteArrayOf(0x49.toByte(), 0x49.toByte(), 0x2A.toByte(), 0x00.toByte())
    private val DNG_HEADER_MM =
        byteArrayOf(0x4D.toByte(), 0x4D.toByte(), 0x00.toByte(), 0x2A.toByte())
    private val DNG_HEADER_LENGTH = DNG_HEADER_II.size

    /**
     * Checks if imageHeaderBytes starts with 'II' or 'MM' then followed by a word containing 42
     * respecting the binary order. If headerSize is lower than 4 false is returned. Description of
     * DNG format can be found here: [
     * ](https://www.adobe.com/content/dam/acom/en/products/photoshop/pdfs/dng_spec_1.4.0.0.pdf) *
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes starts with ('II' or 'MM') then 42 and headerSize >= 4
     */
    private fun isDngHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean =
        headerSize >= DNG_HEADER_LENGTH &&
            (ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, DNG_HEADER_II) ||
                ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, DNG_HEADER_MM))

    /**
     * These are the first 4 bytes of a binary XML file. We can only support binary XML files and
     * not raw XML files because Android explicitly disallows raw XML files when inflating
     * drawables. Binary XML files are created at build time by Android's AAPT.
     *
     * @see
     *   https://developer.android.com/reference/android/view/LayoutInflater#inflate(org.xmlpull.v1.XmlPullParser,%20android.view.ViewGroup)
     */
    private val BINARY_XML_HEADER: ByteArray =
        byteArrayOf(
            3.toByte(),
            0.toByte(),
            8.toByte(),
            0.toByte(),
        )
    private const val BINARY_XML_HEADER_LENGTH: Int = 4

    private fun isBinaryXmlHeader(headerBytes: ByteArray, headerSize: Int): Boolean {
      return headerSize >= BINARY_XML_HEADER_LENGTH &&
          ImageFormatCheckerUtils.startsWithPattern(headerBytes, BINARY_XML_HEADER)
    }

    /** AVIF specific constants */
    private val AVIF_HEADER_PREFIX: ByteArray = ImageFormatCheckerUtils.asciiBytes("ftyp")
    private val AVIF_HEADER_SUFFIX: ByteArray = ImageFormatCheckerUtils.asciiBytes("avif")
    private const val AVIF_HEADER_LENGTH = 12

    /**
     * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for an AVIF
     * image. AVIF is a subtype of HEIF with the brand "avif".
     *
     * @param imageHeaderBytes
     * @param headerSize
     * @return true if imageHeaderBytes is a valid header for an AVIF image
     */
    private fun isAvifHeader(imageHeaderBytes: ByteArray, headerSize: Int): Boolean {
      if (headerSize < AVIF_HEADER_LENGTH) {
        return false
      }
      val boxLength = getBoxLength(imageHeaderBytes)
      if (boxLength < 8) {
        return false
      }
      if (!ImageFormatCheckerUtils.hasPatternAt(imageHeaderBytes, AVIF_HEADER_PREFIX, 4)) {
        return false
      }
      return ImageFormatCheckerUtils.hasPatternAt(imageHeaderBytes, AVIF_HEADER_SUFFIX, 8)
    }

    /**
     * Helper function to extract the box length from the first four bytes.
     *
     * @param bytes The byte array containing the header.
     * @return The box length as an integer.
     */
    private fun getBoxLength(bytes: ByteArray): Int {
      if (bytes.size < 4) return -1
      return ((bytes[0].toInt() and 0xFF) shl 24) or
          ((bytes[1].toInt() and 0xFF) shl 16) or
          ((bytes[2].toInt() and 0xFF) shl 8) or
          (bytes[3].toInt() and 0xFF)
    }
  }
}
