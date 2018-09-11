/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat;

import com.facebook.common.internal.Preconditions;
import java.io.UnsupportedEncodingException;

/**
 * {@link ImageFormatChecker} utility methods
 */
public class ImageFormatCheckerUtils {

  /**
   * Helper method that transforms provided string into it's byte representation
   * using ASCII encoding.
   * @param value the string to use
   * @return byte array representing ascii encoded value
   */
  public static byte[] asciiBytes(String value) {
    Preconditions.checkNotNull(value);
    try {
      return value.getBytes("ASCII");
    } catch (UnsupportedEncodingException uee) {
      // won't happen
      throw new RuntimeException("ASCII not found!", uee);
    }
  }

  /**
   * Checks if byteArray interpreted as sequence of bytes starts with pattern
   * starting at position equal to offset.
   * @param byteArray the byte array to be checked
   * @param pattern the pattern to check
   * @return true if byteArray starts with pattern
   */
  public static boolean startsWithPattern(
      final byte[] byteArray,
      final byte[] pattern) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkNotNull(pattern);
    if (pattern.length > byteArray.length) {
      return false;
    }

    for (int i = 0; i < pattern.length; ++i) {
      if (byteArray[i] != pattern[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if byteArray interpreted as sequence of bytes contains the pattern.
   * @param byteArray the byte array to be checked
   * @param pattern the pattern to check
   * @return index of beginning of pattern, if found; otherwise -1
   */
  public static int indexOfPattern(
      final byte[] byteArray,
      final int byteArrayLen,
      final byte[] pattern,
      final int patternLen) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkNotNull(pattern);
    if (patternLen > byteArrayLen) {
      return -1;
    }

    byte first = pattern[0];
    int max = byteArrayLen - patternLen;

    for (int i = 0; i <= max; i++) {
      // Look for first byte
      if (byteArray[i] != first) {
        while (++i <= max && byteArray[i] != first) {
        }
      }

      // Found first byte, now look for the rest
      if (i <= max) {
        int j = i + 1;
        int end = j + patternLen - 1;
        for (int k = 1; j < end && byteArray[j] == pattern[k]; j++, k++) {
        }

        if (j == end) {
          // found whole pattern
          return i;
        }
      }
    }
    return -1;
  }

  private ImageFormatCheckerUtils() {}
}
