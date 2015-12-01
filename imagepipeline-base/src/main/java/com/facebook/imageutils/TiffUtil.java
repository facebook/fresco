/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageutils;

import android.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

import com.facebook.common.logging.FLog;

/**
 * Util for getting exif orientation from a jpeg stored as a byte array.
 */
class TiffUtil {

  private static final Class<?> TAG = TiffUtil.class;

  public static final int TIFF_BYTE_ORDER_BIG_END = 0x4D4D002A;
  public static final int TIFF_BYTE_ORDER_LITTLE_END = 0x49492A00;
  public static final int TIFF_TAG_ORIENTATION = 0x0112;
  public static final int TIFF_TYPE_SHORT = 3;

  /**
   * Determines auto-rotate angle based on orientation information.
   * @param orientation orientation information read from APP1 EXIF (TIFF) block.
   * @return orientation: 1/3/6/8 -> 0/180/90/270.
   */
  public static int getAutoRotateAngleFromOrientation(int orientation) {
    switch (orientation) {
      case ExifInterface.ORIENTATION_NORMAL:
        return 0;
      case ExifInterface.ORIENTATION_ROTATE_180:
        return 180;
      case ExifInterface.ORIENTATION_ROTATE_90:
        return 90;
      case ExifInterface.ORIENTATION_ROTATE_270:
        return 270;
    }
    FLog.i(TAG, "Unsupported orientation");
    return 0;
  }

  /**
   * Reads orientation information from TIFF data.
   * @param is the input stream of TIFF data
   * @param length length of the TIFF data
   * @return orientation information (1/3/6/8 on success, 0 if not found)
   */
  public static int readOrientationFromTIFF(InputStream is, int length) throws IOException {
    // read tiff header
    TiffHeader tiffHeader = new TiffHeader();
    length = readTiffHeader(is, length, tiffHeader);

    // move to the first IFD
    // offset is relative to the beginning of the TIFF data
    // and we already consumed the first 8 bytes of header
    int toSkip = tiffHeader.firstIfdOffset - 8;
    if (length == 0 || toSkip > length) {
      return 0;
    }
    is.skip(toSkip);
    length -= toSkip;

    // move to the entry with orientation tag
    length = moveToTiffEntryWithTag(is, length, tiffHeader.isLittleEndian, TIFF_TAG_ORIENTATION);

    // read orientation
    return getOrientationFromTiffEntry(is, length, tiffHeader.isLittleEndian);
  }

  /**
   * Structure that holds TIFF header.
   */
  private static class TiffHeader {
    boolean isLittleEndian;
    int byteOrder;
    int firstIfdOffset;
  }

  /**
   * Reads the TIFF header to the provided structure.
   * @param is the input stream of TIFF data
   * @param length length of the TIFF data
   * @return remaining length of the data on success, 0 on failure
   * @throws IOException
   */
  private static int readTiffHeader(InputStream is, int length, TiffHeader tiffHeader)
      throws IOException {
    if (length <= 8) {
      return 0;
    }

    // read the byte order
    tiffHeader.byteOrder = StreamProcessor.readPackedInt(is, 4, false);
    length -= 4;
    if (tiffHeader.byteOrder != TIFF_BYTE_ORDER_LITTLE_END &&
        tiffHeader.byteOrder != TIFF_BYTE_ORDER_BIG_END) {
      FLog.e(TAG, "Invalid TIFF header");
      return 0;
    }
    tiffHeader.isLittleEndian = (tiffHeader.byteOrder == TIFF_BYTE_ORDER_LITTLE_END);

    // read the offset of the first IFD and check if it is reasonable
    tiffHeader.firstIfdOffset = StreamProcessor.readPackedInt(is, 4, tiffHeader.isLittleEndian);
    length -= 4;
    if (tiffHeader.firstIfdOffset < 8 || tiffHeader.firstIfdOffset - 8 > length) {
      FLog.e(TAG, "Invalid offset");
      return 0;
    }

    return length;
  }

  /**
   * Positions the given input stream to the entry that has a specified tag. Tag will be consumed.
   * @param is the input stream of TIFF data positioned to the beginning of an IFD.
   * @param length length of the available data in the given input stream.
   * @param isLittleEndian whether the TIFF data is stored in little or big endian format
   * @param tagToFind tag to find
   * @return remaining length of the data on success, 0 on failure
   */
  private static int moveToTiffEntryWithTag(
      InputStream is,
      int length,
      boolean isLittleEndian,
      int tagToFind)
      throws IOException {
    if (length < 14) {
      return 0;
    }
    // read the number of entries and go through all of them
    // each IFD entry has length of 12 bytes and is composed of
    // {TAG [2], TYPE [2], COUNT [4], VALUE/OFFSET [4]}
    int numEntries = StreamProcessor.readPackedInt(is, 2, isLittleEndian);
    length -= 2;
    while (numEntries-- > 0 && length >= 12) {
      int tag = StreamProcessor.readPackedInt(is, 2, isLittleEndian);
      length -= 2;
      if (tag == tagToFind) {
        return length;
      }
      is.skip(10);
      length -= 10;
    }
    return 0;
  }

  /**
   * Reads the orientation information from the TIFF entry.
   * It is assumed that the entry has a TIFF orientation tag and that tag has already been consumed.
   * @param is the input stream positioned at the TIFF entry with tag already being consumed
   * @param isLittleEndian whether the TIFF data is stored in little or big endian format
   * @return Orientation value in TIFF IFD entry.
   */
  private static int getOrientationFromTiffEntry(InputStream is, int length, boolean isLittleEndian)
      throws IOException {
    if (length < 10) {
      return 0;
    }
    // orientation entry has type = short
    int type = StreamProcessor.readPackedInt(is, 2, isLittleEndian);
    if (type != TIFF_TYPE_SHORT) {
      return 0;
    }
    // orientation entry has count = 1
    int count = StreamProcessor.readPackedInt(is, 4, isLittleEndian);
    if (count != 1) {
      return 0;
    }
    int value = StreamProcessor.readPackedInt(is, 2, isLittleEndian);
    int padding = StreamProcessor.readPackedInt(is, 2, isLittleEndian);
    return value;
  }

}
