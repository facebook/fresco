/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat

import java.io.UnsupportedEncodingException

/** [ImageFormatChecker] utility methods */
object ImageFormatCheckerUtils {

  /**
   * Helper method that transforms provided string into it's byte representation using ASCII
   * encoding.
   *
   * @param value the string to use
   * @return byte array representing ascii encoded value
   */
  @JvmStatic
  fun asciiBytes(value: String): ByteArray {
    return try {
      value.toByteArray(charset("ASCII"))
    } catch (uee: UnsupportedEncodingException) {
      // won't happen
      throw RuntimeException("ASCII not found!", uee)
    }
  }

  /**
   * Checks if byteArray interpreted as sequence of bytes starts with pattern starting at position
   * equal to offset.
   *
   * @param byteArray the byte array to be checked
   * @param pattern the pattern to check
   * @return true if byteArray starts with pattern
   */
  @JvmStatic
  fun startsWithPattern(byteArray: ByteArray, pattern: ByteArray): Boolean =
      hasPatternAt(byteArray, pattern, 0)

  /**
   * Checks if byteArray interpreted as sequence of bytes starts with pattern starting at position
   * equal to offset.
   *
   * @param byteArray the byte array to be checked
   * @param pattern the pattern to check
   * @return true if byteArray starts with pattern
   */
  @JvmStatic
  fun hasPatternAt(byteArray: ByteArray, pattern: ByteArray, offset: Int): Boolean {
    if (offset + pattern.size > byteArray.size) {
      return false
    }
    return pattern.indices.none { i -> byteArray[offset + i] != pattern[i] }
  }

  /**
   * Checks if byteArray interpreted as sequence of bytes contains the pattern.
   *
   * @param byteArray the byte array to be checked
   * @param pattern the pattern to check
   * @return index of beginning of pattern, if found; otherwise -1
   */
  @JvmStatic
  fun indexOfPattern(
      byteArray: ByteArray,
      byteArrayLen: Int,
      pattern: ByteArray,
      patternLen: Int
  ): Int {
    checkNotNull(byteArray)
    checkNotNull(pattern)
    if (patternLen > byteArrayLen) {
      return -1
    }
    val first = pattern[0]
    val max = byteArrayLen - patternLen
    var i = 0
    while (i <= max) {

      // Look for first byte
      if (byteArray[i] != first) {
        while (++i <= max && byteArray[i] != first) {}
      }

      // Found first byte, now look for the rest
      if (i <= max) {
        var j = i + 1
        val end = j + patternLen - 1
        var k = 1
        while (j < end && byteArray[j] == pattern[k]) {
          j++
          k++
        }
        if (j == end) {
          // found whole pattern
          return i
        }
      }
      i++
    }
    return -1
  }
}
