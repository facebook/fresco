/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageutils;

import java.lang.String;
import java.lang.StringBuffer;

import com.facebook.common.util.Hex;

/**
 * Provide test use functions for JfifUtilTest.
 */
public class JfifTestUtils {

  // Markers
  public static final String SOI = "FFD8";
  public static final String DQT_MARKER = "FFDB";
  public static final String DHT_MARKER = "FFC4";
  public static final String DRI_MARKER = "FFDD";
  public static final String SOF_MARKER = "FFC0";
  public static final String SOS_MARKER = "FFDA";
  public static final String EOI = "FFD9";
  public static final String APP0_MARKER = "FFE0";
  public static final String APP1_MARKER = "FFE1";
  public static final String APP2_MARKER = "FFE2";

  // Test blocks
  public static final String DQT = DQT_MARKER + "0004 0000"; // content length 4
  public static final String DHT = DHT_MARKER + "0006 0000 0000"; // content length 6
  public static final String DRI = DRI_MARKER + "0004 0000"; // content length 4, optional
  public static final String SOF = SOF_MARKER + "0006 0000 0000"; // content length 6
  public static final String SOS = SOS_MARKER + "0004 0000"; // content length 4
  public static final String APP0 = APP0_MARKER + "0004 0000"; // content length 4, optional
  public static final String APP2 = APP2_MARKER + "0004 0000"; // content length 4, optional

  // APP1 related headers and magic number.
  public static final String APP1_EXIF_MAGIC = "4578 6966 0000";
  public static final String TIFF_HEADER_LE = "4949 2A00 0800 0000";
  public static final String TIFF_HEADER_BE = "4D4D 002A 0000 0008";
  // IFD related content constant definition
  public static final int IFD_ENTRY_ORI_TAG = 0x0112;
  public static final int IFD_ENTRY_TAG_1 = 0x011A;
  public static final int IFD_ENTRY_TAG_2 = 0x011B;
  public static final int IFD_ENTRY_TAG_3 = 0x011C;
  public static final int TYPE_SHORT = 3;

  public static int numBytes(String data) {
    return data.replaceAll(" ", "").length() / 2;
  }

  public static byte[] hexStringToByteArray(String s) {
    String noSpaceString = s.replaceAll(" ", "");
    byte[] data = Hex.decodeHex(noSpaceString);
    return data;
  }

  public static String encodeInt2HexString(int value, int length, boolean littleEndian) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < length; i++) {
      int oneByte = value & 0xFF;
      if (littleEndian) {
        sb.append(Hex.byte2Hex(oneByte));
      } else {
        sb.insert(0, Hex.byte2Hex(oneByte));
      }
      value = value >> 8;
    }
    return sb.toString();
  }

  public static String makeSOFSection(int length, int bitDepth, int width, int height) {
    return SOF_MARKER + encodeInt2HexString(length, 2, false) +
        encodeInt2HexString(bitDepth, 1, false) +
        encodeInt2HexString(height, 2, false) +
        encodeInt2HexString(width, 2, false) +
        encodeInt2HexString(0, length - 7, false);
  }

  public static String makeOrientationEntry(int orientation, boolean isLittleEnd) {
    return makeIfdEntry(
        IFD_ENTRY_ORI_TAG, TYPE_SHORT, 1, orientation, 2, isLittleEnd);
  }

  public static String makeIfdEntry(
      int tag,
      int type,
      int count,
      int value,
      int valueNumBytes,
      boolean littleEndian) {
    return encodeInt2HexString(tag, 2, littleEndian) +
        encodeInt2HexString(type, 2, littleEndian) +
        encodeInt2HexString(count, 4, littleEndian) +
        encodeInt2HexString(value, valueNumBytes, littleEndian) +
        encodeInt2HexString(0, 4 - valueNumBytes, littleEndian);
  }

  public static String makeIfd(String[] IfdEntries, int nextEntryOffset, boolean littleEndian) {
    String ret = encodeInt2HexString(IfdEntries.length, 2, littleEndian);
    for (int i = 0; i < IfdEntries.length; i++) {
      ret += IfdEntries[i];
    }
    ret += encodeInt2HexString(nextEntryOffset, 4, littleEndian);
    return ret;
  }

  public static String makeTiff(String ifd, boolean littleEndian) {
    String ret = littleEndian ? TIFF_HEADER_LE : TIFF_HEADER_BE;
    return ret + ifd;
  }

  public static String makeAPP1_EXIF(String tiff) {
    String app1Length = encodeInt2HexString(numBytes(tiff) + 8, 2, false);
    return APP1_MARKER + app1Length + APP1_EXIF_MAGIC + tiff;
  }

  public static String makeTestImageWithAPP1(String APP1) {
    return SOI + APP0 + APP1 + DQT + DHT + SOF + SOS + EOI;
  }
}
