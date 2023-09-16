/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils

import android.media.ExifInterface
import com.facebook.common.logging.FLog
import java.io.IOException
import java.io.InputStream

/** Util for getting exif orientation from a jpeg stored as a byte array. */
internal object TiffUtil {

  private val TAG: Class<*> = TiffUtil::class.java
  const val TIFF_BYTE_ORDER_BIG_END: Int = 0x4D4D002A
  const val TIFF_BYTE_ORDER_LITTLE_END: Int = 0x49492A00
  const val TIFF_TAG_ORIENTATION: Int = 0x0112
  const val TIFF_TYPE_SHORT: Int = 3

  /**
   * Determines auto-rotate angle based on orientation information.
   *
   * @param orientation orientation information read from APP1 EXIF (TIFF) block.
   * @return orientation: 1/3/6/8 -> 0/180/90/270. Returns 0 for inverted orientations (2/4/5/7).
   */
  @JvmStatic
  fun getAutoRotateAngleFromOrientation(orientation: Int): Int =
      when (orientation) {
        ExifInterface.ORIENTATION_NORMAL,
        ExifInterface.ORIENTATION_UNDEFINED -> 0
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
      }

  /**
   * Reads orientation information from TIFF data.
   *
   * @param stream the input stream of TIFF data
   * @param length length of the TIFF data
   * @return orientation information (1/3/6/8 on success, 0 if not found)
   */
  @JvmStatic
  @Throws(IOException::class)
  fun readOrientationFromTIFF(stream: InputStream, length: Int): Int {
    // read tiff header
    val tiffHeader = TiffHeader()
    var remainingLength = readTiffHeader(stream, length, tiffHeader)

    // move to the first IFD
    // offset is relative to the beginning of the TIFF data
    // and we already consumed the first 8 bytes of header
    val toSkip = tiffHeader.firstIfdOffset - 8
    if (remainingLength == 0 || toSkip > remainingLength) {
      return 0
    }
    stream.skip(toSkip.toLong())
    remainingLength -= toSkip

    // move to the entry with orientation tag
    remainingLength =
        moveToTiffEntryWithTag(
            stream, remainingLength, tiffHeader.isLittleEndian, TIFF_TAG_ORIENTATION)

    // read orientation
    return getOrientationFromTiffEntry(stream, remainingLength, tiffHeader.isLittleEndian)
  }

  /**
   * Reads the TIFF header to the provided structure.
   *
   * @param stream the input stream of TIFF data
   * @param length length of the TIFF data
   * @return remaining length of the data on success, 0 on failure
   * @throws IOException
   */
  @Throws(IOException::class)
  private fun readTiffHeader(stream: InputStream, length: Int, tiffHeader: TiffHeader): Int {
    if (length <= 8) {
      return 0
    }

    var remainingLength = length

    // read the byte order
    tiffHeader.byteOrder = StreamProcessor.readPackedInt(stream, 4, false)
    remainingLength -= 4
    if (tiffHeader.byteOrder != TIFF_BYTE_ORDER_LITTLE_END &&
        tiffHeader.byteOrder != TIFF_BYTE_ORDER_BIG_END) {
      FLog.e(TAG, "Invalid TIFF header")
      return 0
    }
    tiffHeader.isLittleEndian = tiffHeader.byteOrder == TIFF_BYTE_ORDER_LITTLE_END

    // read the offset of the first IFD and check if it is reasonable
    tiffHeader.firstIfdOffset = StreamProcessor.readPackedInt(stream, 4, tiffHeader.isLittleEndian)
    remainingLength -= 4
    if (tiffHeader.firstIfdOffset < 8 || tiffHeader.firstIfdOffset - 8 > remainingLength) {
      FLog.e(TAG, "Invalid offset")
      return 0
    }
    return remainingLength
  }

  /**
   * Positions the given input stream to the entry that has a specified tag. Tag will be consumed.
   *
   * @param stream the input stream of TIFF data positioned to the beginning of an IFD.
   * @param length length of the available data in the given input stream.
   * @param isLittleEndian whether the TIFF data is stored in little or big endian format
   * @param tagToFind tag to find
   * @return remaining length of the data on success, 0 on failure
   */
  @Throws(IOException::class)
  private fun moveToTiffEntryWithTag(
      stream: InputStream,
      length: Int,
      isLittleEndian: Boolean,
      tagToFind: Int
  ): Int {
    if (length < 14) {
      return 0
    }

    var remainingLength = length
    // read the number of entries and go through all of them
    // each IFD entry has length of 12 bytes and is composed of
    // {TAG [2], TYPE [2], COUNT [4], VALUE/OFFSET [4]}
    var numEntries = StreamProcessor.readPackedInt(stream, 2, isLittleEndian)
    remainingLength -= 2

    while (numEntries-- > 0 && remainingLength >= 12) {
      val tag = StreamProcessor.readPackedInt(stream, 2, isLittleEndian)
      remainingLength -= 2
      if (tag == tagToFind) {
        return remainingLength
      }
      stream.skip(10)
      remainingLength -= 10
    }

    return 0
  }

  /**
   * Reads the orientation information from the TIFF entry. It is assumed that the entry has a TIFF
   * orientation tag and that tag has already been consumed.
   *
   * @param stream the input stream positioned at the TIFF entry with tag already being consumed
   * @param isLittleEndian whether the TIFF data is stored in little or big endian format
   * @return Orientation value in TIFF IFD entry.
   */
  @Throws(IOException::class)
  private fun getOrientationFromTiffEntry(
      stream: InputStream,
      length: Int,
      isLittleEndian: Boolean
  ): Int {
    if (length < 10) {
      return 0
    }
    // orientation entry has type = short
    val type = StreamProcessor.readPackedInt(stream, 2, isLittleEndian)
    if (type != TIFF_TYPE_SHORT) {
      return 0
    }
    // orientation entry has count = 1
    val count = StreamProcessor.readPackedInt(stream, 4, isLittleEndian)
    if (count != 1) {
      return 0
    }
    return StreamProcessor.readPackedInt(stream, 2, isLittleEndian)
  }

  /** Structure that holds TIFF header. */
  private class TiffHeader {
    var isLittleEndian = false
    var byteOrder = 0
    var firstIfdOffset = 0
  }
}
