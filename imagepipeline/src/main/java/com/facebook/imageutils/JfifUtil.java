/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageutils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Rect;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;

/**
 * Util for obtaining information from JPEG file.
 */
public class JfifUtil {

  private static final Class<?> TAG = JfifUtil.class;

  /**
   * Definitions of jpeg markers as well as overall description of jpeg file format can be found
   * here: <a href="http://www.w3.org/Graphics/JPEG/itu-t81.pdf">Recommendation T.81</a>
   */
  public static final int MARKER_FIRST_BYTE = 0xFF;
  public static final int MARKER_ESCAPE_BYTE = 0x00;
  public static final int MARKER_SOI = 0xD8;
  public static final int MARKER_TEM = 0x01;
  public static final int MARKER_EOI = 0xD9;
  public static final int MARKER_SOS = 0xDA;
  public static final int MARKER_APP1 = 0xE1;
  public static final int MARKER_SOFn = 0xC0;
  public static final int MARKER_RST0 = 0xD0;
  public static final int MARKER_RST7 = 0xD7;
  public static final int APP1_EXIF_MAGIC = 0x45786966;

  private JfifUtil() {
  }

  /**
   * Determines auto-rotate angle based on orientation information.
   * @param orientation orientation information, one of {1, 3, 6, 8}.
   * @return orientation: 1/3/6/8 -> 0/180/90/270.
   */
  public static int getAutoRotateAngleFromOrientation(int orientation) {
    return TiffUtil.getAutoRotateAngleFromOrientation(orientation);
  }

  /**
   * Gets orientation information from jpeg byte array.
   * @param jpeg the input byte array of jpeg image
   * @return orientation: 1/8/3/6. Returns 0 if there is no valid orientation information.
   */
  public static int getOrientation(byte[] jpeg) {
    // wrapping with ByteArrayInputStream is cheap and we don't have duplicate implementation
    return getOrientation(new ByteArrayInputStream(jpeg));
  }

  /**
   * Get orientation information from jpeg input stream.
   * @param is the input stream of jpeg image
   * @return orientation: 1/8/3/6. Returns 0 if there is no valid orientation information.
   */
  public static int getOrientation(InputStream is) {
    try {
      int length = moveToAPP1EXIF(is);
      if (length == 0) {
        return 0; // unknown orientation
      }
      return TiffUtil.readOrientationFromTIFF(is, length);
    } catch (IOException ioe) {
      return 0;
    }
  }

  /**
   * Get image width and height from jpeg header
   * @param jpeg the input byte array of jpeg image
   * @return dimensions of the image in form of Rect.
   */
  public static Rect getDimensions(byte[] jpeg) {
    // wrapping with ByteArrayInputStream is cheap and we don't have duplicate implementation
    return getDimensions(new ByteArrayInputStream(jpeg));
  }

  /**
   * Get image width and height from jpeg header
   * @param is the input stream of jpeg image
   * @return dimensions of the image in form of Rect
   */
  public static Rect getDimensions(InputStream is) {
    try {
      if (moveToMarker(is, MARKER_SOFn)) {
        // read block length
        // subtract 2 as length contain SIZE field we just read
        int length = StreamProcessor.readPackedInt(is, 2, false) - 2;
        if (length > 6) {
          // SOFn structure: 0xFFCn|length(2)|bitDepth(1)|height(2)|width(2)|...
          int bitDepth = StreamProcessor.readPackedInt(is, 1, false);
          int height = StreamProcessor.readPackedInt(is, 2, false);
          int width = StreamProcessor.readPackedInt(is, 2, false);
          return new Rect(0, 0, width, height);
        }
      }
    } catch (IOException ioe) {
      FLog.e(TAG, ioe, "%x: getDimensions", is.hashCode());
      // log and return null.
    }
    return null;
  }

  /**
   *  Reads the content of the input stream until specified marker is found. Marker will be
   *  consumed and the input stream will be positioned after the specified marker.
   *  @param is the input stream to read bytes from
   *  @param markerToFind the marker we are looking for
   *  @return boolean: whether or not we found the expected marker from input stream.
   */
  public static boolean moveToMarker(InputStream is, int markerToFind) throws IOException {
    Preconditions.checkNotNull(is);
    // ISO/IEC 10918-1:1993(E)
    while (StreamProcessor.readPackedInt(is, 1, false) == MARKER_FIRST_BYTE) {
      int marker = MARKER_FIRST_BYTE;
      while (marker == MARKER_FIRST_BYTE) {
        marker = StreamProcessor.readPackedInt(is, 1, false);
      }

      if (markerToFind == MARKER_SOFn && isSOFn(marker)) {
        return true;
      }
      if (marker == markerToFind) {
        return true;
      }

      // Check if the marker is SOI or TEM. These two don't have length field, so we skip it.
      if (marker == MARKER_SOI || marker == MARKER_TEM) {
        continue;
      }

      // Check if the marker is EOI or SOS. We will stop reading since metadata markers don't
      // come after these two markers.
      if (marker == MARKER_EOI || marker == MARKER_SOS) {
        return false;
      }

      // read block length
      // subtract 2 as length contain SIZE field we just read
      int length = StreamProcessor.readPackedInt(is, 2, false) - 2;
      // Skip other markers.
      is.skip(length);
    }
    return false;
  }

  private static boolean isSOFn(int marker) {
    // There are no SOF4, SOF8, SOF12
    switch (marker) {
      case 0xC0:
      case 0xC1:
      case 0xC2:
      case 0xC3:
      case 0xC5:
      case 0xC6:
      case 0xC7:
      case 0xC9:
      case 0xCA:
      case 0xCB:
      case 0xCD:
      case 0xCE:
      case 0xCF:
        return true;
      default:
        return false;
    }
  }

  /**
   * Positions the given input stream to the beginning of the EXIF data in the JPEG APP1 block.
   * @param is the input stream of jpeg image
   * @return length of EXIF data
   */
  private static int moveToAPP1EXIF(InputStream is) throws IOException {
    if (moveToMarker(is, MARKER_APP1)) {
      // read block length
      // subtract 2 as length contain SIZE field we just read
      int length = StreamProcessor.readPackedInt(is, 2, false) - 2;
      if (length > 6) {
        int magic = StreamProcessor.readPackedInt(is, 4, false);
        length -= 4;
        int zero = StreamProcessor.readPackedInt(is, 2, false);
        length -= 2;
        if (magic == APP1_EXIF_MAGIC && zero == 0) {
          // JEITA CP-3451 Exif Version 2.2
          return length;
        }
      }
    }
    return 0;
  }
}
