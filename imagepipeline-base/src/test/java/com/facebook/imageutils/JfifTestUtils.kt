/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils

import com.facebook.common.util.Hex.byte2Hex
import com.facebook.common.util.Hex.decodeHex

/** Provide test use functions for JfifUtilTest. */
object JfifTestUtils {
  // Markers
  const val SOI: String = "FFD8"
  const val DQT_MARKER: String = "FFDB"
  const val DHT_MARKER: String = "FFC4"
  const val DRI_MARKER: String = "FFDD"
  const val SOF_MARKER: String = "FFC0"
  const val SOS_MARKER: String = "FFDA"
  const val EOI: String = "FFD9"
  const val APP0_MARKER: String = "FFE0"
  const val APP1_MARKER: String = "FFE1"
  const val APP2_MARKER: String = "FFE2"

  // Test blocks
  val DQT: String = DQT_MARKER + "0004 0000" // content length 4
  val DHT: String = DHT_MARKER + "0006 0000 0000" // content length 6
  val DRI: String = DRI_MARKER + "0004 0000" // content length 4, optional
  val SOF: String = SOF_MARKER + "0006 0000 0000" // content length 6
  val SOS: String = SOS_MARKER + "0004 0000" // content length 4
  val APP0: String = APP0_MARKER + "0004 0000" // content length 4, optional
  val APP2: String = APP2_MARKER + "0004 0000" // content length 4, optional

  // APP1 related headers and magic number.
  const val APP1_EXIF_MAGIC: String = "4578 6966 0000"
  const val TIFF_HEADER_LE: String = "4949 2A00 0800 0000"
  const val TIFF_HEADER_BE: String = "4D4D 002A 0000 0008"
  // IFD related content constant definition
  const val IFD_ENTRY_ORI_TAG: Int = 0x0112
  const val IFD_ENTRY_TAG_1: Int = 0x011A
  const val IFD_ENTRY_TAG_2: Int = 0x011B
  const val IFD_ENTRY_TAG_3: Int = 0x011C
  const val TYPE_SHORT: Int = 3

  @JvmStatic
  fun numBytes(data: String): Int {
    return data.replace(" ".toRegex(), "").length / 2
  }

  @JvmStatic
  fun hexStringToByteArray(s: String): ByteArray {
    val noSpaceString = s.replace(" ".toRegex(), "")
    val data = decodeHex(noSpaceString)
    return data
  }

  @JvmStatic
  fun encodeInt2HexString(value: Int, length: Int, littleEndian: Boolean): String {
    var value = value
    val sb = StringBuffer()
    for (i in 0..<length) {
      val oneByte = value and 0xFF
      if (littleEndian) {
        sb.append(byte2Hex(oneByte))
      } else {
        sb.insert(0, byte2Hex(oneByte))
      }
      value = value shr 8
    }
    return sb.toString()
  }

  fun makeSOFSection(length: Int, bitDepth: Int, width: Int, height: Int): String {
    return (SOF_MARKER +
        encodeInt2HexString(length, 2, false) +
        encodeInt2HexString(bitDepth, 1, false) +
        encodeInt2HexString(height, 2, false) +
        encodeInt2HexString(width, 2, false) +
        encodeInt2HexString(0, length - 7, false))
  }

  @JvmStatic
  fun makeOrientationEntry(orientation: Int, isLittleEnd: Boolean): String {
    return makeIfdEntry(IFD_ENTRY_ORI_TAG, TYPE_SHORT, 1, orientation, 2, isLittleEnd)
  }

  @JvmStatic
  fun makeIfdEntry(
      tag: Int,
      type: Int,
      count: Int,
      value: Int,
      valueNumBytes: Int,
      littleEndian: Boolean
  ): String {
    return (encodeInt2HexString(tag, 2, littleEndian) +
        encodeInt2HexString(type, 2, littleEndian) +
        encodeInt2HexString(count, 4, littleEndian) +
        encodeInt2HexString(value, valueNumBytes, littleEndian) +
        encodeInt2HexString(0, 4 - valueNumBytes, littleEndian))
  }

  @JvmStatic
  fun makeIfd(IfdEntries: Array<String?>, nextEntryOffset: Int, littleEndian: Boolean): String {
    var ret = encodeInt2HexString(IfdEntries.size, 2, littleEndian)
    for (i in IfdEntries.indices) {
      ret += IfdEntries[i]
    }
    ret += encodeInt2HexString(nextEntryOffset, 4, littleEndian)
    return ret
  }

  @JvmStatic
  fun makeTiff(ifd: String, littleEndian: Boolean): String {
    val ret = if (littleEndian) TIFF_HEADER_LE else TIFF_HEADER_BE
    return ret + ifd
  }

  @JvmStatic
  fun makeAPP1_EXIF(tiff: String): String {
    val app1Length = encodeInt2HexString(numBytes(tiff) + 8, 2, false)
    return APP1_MARKER + app1Length + APP1_EXIF_MAGIC + tiff
  }

  @JvmStatic
  fun makeTestImageWithAPP1(APP1: String?): String {
    return SOI + APP0 + APP1 + DQT + DHT + SOF + SOS + EOI
  }
}
