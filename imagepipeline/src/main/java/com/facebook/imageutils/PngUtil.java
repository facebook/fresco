/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imageutils;

import android.graphics.Rect;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;

import java.io.IOException;
import java.io.InputStream;

/**
 * Util for obtaining information from PNG file.
 */
public class PngUtil {

  private static final String TAG = PngUtil.class.getName();

  /**
   * Information about the png file format can be found here:
   * <a href="http://www.libpng.org/pub/png/spec/1.2/PNG-Contents.html">PNG Image Format</a>
   */
  private static final int CHUNK_LENGTH_FIELD_SIZE = 4;
  private static final int CHUNK_TYPE_FIELD_SIZE = 4;
  private static final int WIDTH_FIELD_SIZE = 4;
  private static final int HEIGHT_FIELD_SIZE = 4;
  private static final int IHDR_DATA_SIZE = 13;

  /**
   * Every PNG image starts with 8 byte signature consisting of
   * following bytes
   */
  private static final byte[] PNG_HEADER = new byte[] {
      (byte) 0x89,
      'P', 'N', 'G',
      (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};

  /**
   * Get image width and height from png header
   * @param is the input stream of png image
   * @return dimensions of the image in form of Rect
   */
  public static Rect getDimensions(InputStream is) {
    Preconditions.checkNotNull(is);
    try {
      byte[] headerBytes = new byte[PNG_HEADER.length];
      // check the png header bytes
      is.read(headerBytes, 0, PNG_HEADER.length);
      if (!matchBytes(headerBytes, PNG_HEADER)) {
        return null;
      }
      // read IHDR chunk length
      int length = StreamProcessor.readPackedInt(is, CHUNK_LENGTH_FIELD_SIZE, false);
      // If the read length is smaller than the minimum IHDR length return null as the dimensions
      // cant be read.
      if (length >= IHDR_DATA_SIZE) {
        // skip the bytes that specify the chunk type
        is.skip(CHUNK_TYPE_FIELD_SIZE);
        // IHDR structure: width(4)|height(4)|bitDepth(1)|colorType(1)|compressionMethod(1)|
        // filterMethod(1)|interlaceMethod(1)|...
        int width = StreamProcessor.readPackedInt(is, WIDTH_FIELD_SIZE, false);
        int height = StreamProcessor.readPackedInt(is, HEIGHT_FIELD_SIZE, false);
        return new Rect(0, 0, width, height);
      }
    } catch (IOException ioe) {
      FLog.e(TAG, ioe, "unable to read dimensions of PNG image");
      // log and return null.
    }
    return null;
  }

  private static boolean matchBytes(byte[] bytes1, byte[] bytes2) {
    if (bytes1.length != bytes2.length) {
      return false;
    }
    for (int i = 0; i < bytes1.length; i++) {
      if (bytes1[i] != bytes2[i]) {
        return false;
      }
    }
    return true;
  }

}
