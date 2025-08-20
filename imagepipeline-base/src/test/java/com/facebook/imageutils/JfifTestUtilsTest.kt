/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils

import com.facebook.imageutils.JfifTestUtils.APP0
import com.facebook.imageutils.JfifTestUtils.APP1_EXIF_MAGIC
import com.facebook.imageutils.JfifTestUtils.APP1_MARKER
import com.facebook.imageutils.JfifTestUtils.DHT
import com.facebook.imageutils.JfifTestUtils.DQT
import com.facebook.imageutils.JfifTestUtils.EOI
import com.facebook.imageutils.JfifTestUtils.IFD_ENTRY_ORI_TAG
import com.facebook.imageutils.JfifTestUtils.IFD_ENTRY_TAG_1
import com.facebook.imageutils.JfifTestUtils.IFD_ENTRY_TAG_2
import com.facebook.imageutils.JfifTestUtils.IFD_ENTRY_TAG_3
import com.facebook.imageutils.JfifTestUtils.SOF
import com.facebook.imageutils.JfifTestUtils.SOI
import com.facebook.imageutils.JfifTestUtils.SOS
import com.facebook.imageutils.JfifTestUtils.TIFF_HEADER_BE
import com.facebook.imageutils.JfifTestUtils.TIFF_HEADER_LE
import com.facebook.imageutils.JfifTestUtils.TYPE_SHORT
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests [JfifTestUtils] */
@RunWith(RobolectricTestRunner::class)
class JfifTestUtilsTest {

  private val testStr = "0123456789"

  @Test
  fun testMakeSOFSection() {
    assertThat(
            JfifTestUtils.makeSOFSection(
                10, // length
                3, // bit depth
                310, // width
                255, // height
            )
        )
        .isEqualTo("FFC0000A0300FF0136000000")

    assertThat(
            JfifTestUtils.makeSOFSection(
                20, // length
                1, // bit depth
                255, // width
                310, // height
            )
        )
        .isEqualTo("FFC0001401013600FF00000000000000000000000000")
  }

  @Test
  fun testNumBytes() {
    assertThat(JfifTestUtils.numBytes("    3F        ")).isEqualTo(1)
    assertThat(JfifTestUtils.numBytes("1A 2B 3C 4D")).isEqualTo(4)
    assertThat(JfifTestUtils.numBytes("1A2B 3C4D5E6F")).isEqualTo(6)
  }

  @Test
  fun testHexStringToByteArray() {
    assertThat(JfifTestUtils.hexStringToByteArray("    3F        ")).isEqualTo(byteArrayOf(0x3F))
    assertThat(JfifTestUtils.hexStringToByteArray("1A 2B 3C 4D"))
        .isEqualTo(byteArrayOf(0x1A, 0x2B, 0x3C, 0x4D))
    assertThat(JfifTestUtils.hexStringToByteArray("1A2B 3C4D5E6F"))
        .isEqualTo(byteArrayOf(0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F))
  }

  @Test
  fun testEncodeInt2HexString() {
    assertThat(JfifTestUtils.encodeInt2HexString(1534063666, 4, false)).isEqualTo("5B6FF432")
    assertThat(JfifTestUtils.encodeInt2HexString(28660, 2, false)).isEqualTo("6FF4")
    assertThat(JfifTestUtils.encodeInt2HexString(182, 1, false)).isEqualTo("B6")
    assertThat(JfifTestUtils.encodeInt2HexString(1534063666, 4, true)).isEqualTo("32F46F5B")
    assertThat(JfifTestUtils.encodeInt2HexString(28660, 2, true)).isEqualTo("F46F")
    assertThat(JfifTestUtils.encodeInt2HexString(182, 1, true)).isEqualTo("B6")
  }

  @Test
  fun testMakeOrientationEntry() {
    assertThat(JfifTestUtils.makeOrientationEntry(5, false)).isEqualTo("011200030000000100050000")
    assertThat(JfifTestUtils.makeOrientationEntry(5, true)).isEqualTo("120103000100000005000000")
  }

  @Test
  fun testMakeIfdEntry() {
    assertThat(JfifTestUtils.makeIfdEntry(IFD_ENTRY_ORI_TAG, TYPE_SHORT, 1, 6, 2, false))
        .isEqualTo("011200030000000100060000")
    assertThat(JfifTestUtils.makeIfdEntry(IFD_ENTRY_ORI_TAG, TYPE_SHORT, 2, 3, 2, true))
        .isEqualTo("120103000200000003000000")
  }

  @Test
  fun testMakeIfd() {
    // Test big endian
    var ifdEntry1 = JfifTestUtils.makeIfdEntry(IFD_ENTRY_TAG_1, TYPE_SHORT, 1, 255, 2, false)
    var ifdEntry2 = JfifTestUtils.makeIfdEntry(IFD_ENTRY_TAG_2, TYPE_SHORT, 1, 255, 2, false)
    var ifdEntry3 = JfifTestUtils.makeIfdEntry(IFD_ENTRY_TAG_3, TYPE_SHORT, 1, 255, 2, false)
    assertThat(JfifTestUtils.makeIfd(arrayOf(ifdEntry1, ifdEntry2, ifdEntry3), 8, false))
        .isEqualTo(
            "0003" +
                "011A00030000000100FF0000" +
                "011B00030000000100FF0000" +
                "011C00030000000100FF0000" +
                "00000008"
        )

    // Test little endian
    ifdEntry1 = JfifTestUtils.makeIfdEntry(IFD_ENTRY_TAG_1, TYPE_SHORT, 1, 255, 2, true)
    ifdEntry2 = JfifTestUtils.makeIfdEntry(IFD_ENTRY_TAG_2, TYPE_SHORT, 1, 255, 2, true)
    ifdEntry3 = JfifTestUtils.makeIfdEntry(IFD_ENTRY_TAG_3, TYPE_SHORT, 1, 255, 2, true)
    assertThat(JfifTestUtils.makeIfd(arrayOf(ifdEntry1, ifdEntry2, ifdEntry3), 9, true))
        .isEqualTo(
            "0300" +
                "1A01030001000000FF000000" +
                "1B01030001000000FF000000" +
                "1C01030001000000FF000000" +
                "09000000"
        )
  }

  @Test
  fun testMakeTiff() {
    assertThat(JfifTestUtils.makeTiff(testStr, false)).isEqualTo(TIFF_HEADER_BE + testStr)
    assertThat(JfifTestUtils.makeTiff(testStr, true)).isEqualTo(TIFF_HEADER_LE + testStr)
  }

  @Test
  fun testMakeAPP1_EXIF() {
    assertThat(JfifTestUtils.makeAPP1_EXIF(testStr))
        .isEqualTo(APP1_MARKER + "000D" + APP1_EXIF_MAGIC + testStr)
  }

  @Test
  fun testMakeTestImageWithAPP1() {
    assertThat(JfifTestUtils.makeTestImageWithAPP1(testStr))
        .isEqualTo(SOI + APP0 + testStr + DQT + DHT + SOF + SOS + EOI)
  }
}
