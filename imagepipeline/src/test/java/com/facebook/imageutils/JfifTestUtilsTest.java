/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageutils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.facebook.imageutils.JfifTestUtils.*;
import static org.junit.Assert.*;

/**
 * Tests {@link JfifTestUtils}
 */
@RunWith(RobolectricTestRunner.class)
public class JfifTestUtilsTest {

  private String mTestStr = "0123456789";

  @Test
  public void testMakeSOFSection() {
    assertEquals("FFC0000A0300FF0136000000", JfifTestUtils.makeSOFSection(
        10, // length
        3,  // bit depth
        310,  // width
        255));  // height

    assertEquals("FFC0001401013600FF00000000000000000000000000", JfifTestUtils.makeSOFSection(
        20, // length
        1,  // bit depth
        255,  // width
        310));  // height
  }

  @Test
  public void testNumBytes() {
    assertEquals(1, JfifTestUtils.numBytes("    3F        "));
    assertEquals(4, JfifTestUtils.numBytes("1A 2B 3C 4D"));
    assertEquals(6, JfifTestUtils.numBytes("1A2B 3C4D5E6F"));
  }

  @Test
  public void testHexStringToByteArray() {
    assertArrayEquals(new byte[]{0x3F}, JfifTestUtils.hexStringToByteArray("    3F        "));
    assertArrayEquals(new byte[]{0x1A, 0x2B, 0x3C, 0x4D},
        JfifTestUtils.hexStringToByteArray("1A 2B 3C 4D"));
    assertArrayEquals(
        new byte[]{0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F},
        JfifTestUtils.hexStringToByteArray("1A2B 3C4D5E6F"));
  }

  @Test
  public void testEncodeInt2HexString() {
    assertEquals("5B6FF432", JfifTestUtils.encodeInt2HexString(1534063666, 4, false));
    assertEquals("6FF4", JfifTestUtils.encodeInt2HexString(28660, 2, false));
    assertEquals("B6", JfifTestUtils.encodeInt2HexString(182, 1, false));
    assertEquals("32F46F5B", JfifTestUtils.encodeInt2HexString(1534063666, 4, true));
    assertEquals("F46F", JfifTestUtils.encodeInt2HexString(28660, 2, true));
    assertEquals("B6", JfifTestUtils.encodeInt2HexString(182, 1, true));
  }

  @Test
  public void testMakeOrientationEntry() {
    assertEquals("011200030000000100050000", JfifTestUtils.makeOrientationEntry(5, false));
    assertEquals("120103000100000005000000", JfifTestUtils.makeOrientationEntry(5, true));
  }

  @Test
  public void testMakeIfdEntry() {
    assertEquals("011200030000000100060000", JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_ORI_TAG,
        TYPE_SHORT,
        1,
        6,
        2,
        false));
    assertEquals("120103000200000003000000", JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_ORI_TAG,
        TYPE_SHORT,
        2,
        3,
        2,
        true));
  }

  @Test
  public void testMakeIfd() {
    // Test big endian
    String IFD_ENTRY_1 = JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_TAG_1, TYPE_SHORT, 1, 255, 2, false);
    String IFD_ENTRY_2 = JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_TAG_2, TYPE_SHORT, 1, 255, 2, false);
    String IFD_ENTRY_3 = JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_TAG_3, TYPE_SHORT, 1, 255, 2, false);
    assertEquals(
        "0003" +
            "011A00030000000100FF0000" +
            "011B00030000000100FF0000" +
            "011C00030000000100FF0000" +
            "00000008",
        JfifTestUtils.makeIfd(new String[]{IFD_ENTRY_1, IFD_ENTRY_2, IFD_ENTRY_3}, 8, false));

    // Test little endian
    IFD_ENTRY_1 = JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_TAG_1, TYPE_SHORT, 1, 255, 2, true);
    IFD_ENTRY_2 = JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_TAG_2, TYPE_SHORT, 1, 255, 2, true);
    IFD_ENTRY_3 = JfifTestUtils.makeIfdEntry(
        IFD_ENTRY_TAG_3, TYPE_SHORT, 1, 255, 2, true);
    assertEquals(
        "0300" +
            "1A01030001000000FF000000" +
            "1B01030001000000FF000000" +
            "1C01030001000000FF000000" +
            "09000000",
        JfifTestUtils.makeIfd(new String[]{IFD_ENTRY_1, IFD_ENTRY_2, IFD_ENTRY_3}, 9, true));
  }

  @Test
  public void testMakeTiff() {
    assertEquals(TIFF_HEADER_BE + mTestStr, JfifTestUtils.makeTiff(mTestStr, false));
    assertEquals(TIFF_HEADER_LE + mTestStr, JfifTestUtils.makeTiff(mTestStr, true));
  }

  @Test
  public void testMakeAPP1_EXIF() {
    assertEquals(APP1_MARKER + "000D" + APP1_EXIF_MAGIC + mTestStr,
        JfifTestUtils.makeAPP1_EXIF(mTestStr));
  }

  @Test
  public void testMakeTestImageWithAPP1() {
    assertEquals(SOI + APP0 + mTestStr + DQT +
        DHT + SOF + SOS + EOI,
        JfifTestUtils.makeTestImageWithAPP1(mTestStr));
  }
}
