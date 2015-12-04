/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageformat;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;

/**
 * Detects the format of an encoded gif.
 */
public final class GifFormatChecker {

  private static final int FRAME_HEADER_SIZE = 10;

  private GifFormatChecker() {}

  /**
   * Every GIF frame header starts with a 4 byte static sequence consisting of the following bytes
   */
  private static final byte[] FRAME_HEADER_START = new byte[]{
          (byte) 0x00, (byte) 0x21, (byte) 0xF9, (byte) 0x04};
  /**
   * Every GIF frame header ends with a 2 byte static sequence consisting of one of
   * the following two sequences of bytes
   */
  private static final byte[] FRAME_HEADER_END_1 = new byte[]{
          (byte) 0x00, (byte) 0x2C};
  private static final byte[] FRAME_HEADER_END_2 = new byte[]{
          (byte) 0x00, (byte) 0x21};

  /**
   * Checks if source contains more than one frame header in it in order to decide whether a GIF
   * image is animated or not.
   *
   * @return true if source contains more than one frame header in its bytes
   */
  public static boolean isAnimated(InputStream source) {
    final byte[] buffer = new byte[FRAME_HEADER_SIZE];

    try {
      source.read(buffer, 0, FRAME_HEADER_SIZE);

      int offset = 0;
      int frameHeaders = 0;

      // Read bytes into a circular buffer and check if it matches one of the frame header
      // sequences. First byte can be ignored as it will be part of the GIF static header.
      while (source.read(buffer, offset, 1) > 0) {
        // This sequence of bytes might be found in the data section of the file, worst case
        // scenario this method will return true meaning that a static gif is animated.
        if (circularBufferMatchesBytePattern(buffer, offset + 1, FRAME_HEADER_START)
                && (circularBufferMatchesBytePattern(buffer, offset + 9, FRAME_HEADER_END_1)
                || circularBufferMatchesBytePattern(buffer, offset + 9, FRAME_HEADER_END_2))) {
          frameHeaders++;
          if (frameHeaders > 1) {
            return true;
          }
        }
        offset = (offset + 1) % buffer.length;
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }

    return false;
  }

  /**
   * Checks if the byte array matches a pattern.
   *
   * <p>Instead of doing a normal scan, we treat the array as a circular buffer, with 'offset'
   * determining the start point.
   *
   * @return true if match succeeds, false otherwise
   */
  @VisibleForTesting
  static boolean circularBufferMatchesBytePattern(
          byte[] byteArray, int offset, byte[] pattern) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkNotNull(pattern);
    Preconditions.checkArgument(offset >= 0);
    if (pattern.length > byteArray.length) {
      return false;
    }

    for (int i = 0; i < pattern.length; i++) {
      if (byteArray[(i + offset) % byteArray.length] != pattern[i]) {
        return false;
      }
    }
    return true;
  }

}
