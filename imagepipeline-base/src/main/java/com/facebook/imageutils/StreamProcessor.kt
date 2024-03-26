/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils

import java.io.IOException
import java.io.InputStream

/** Util for processing Stream. */
internal object StreamProcessor {
  /**
   * Consumes up to 4 bytes and returns them as int (taking into account endianness). Throws
   * exception if specified number of bytes cannot be consumed.
   *
   * @param stream the input stream to read bytes from
   * @param numBytes the number of bytes to read
   * @param isLittleEndian whether the bytes should be interpreted in little or big endian format
   * @return packed int read from input stream and constructed according to endianness
   */
  @JvmStatic
  @Throws(IOException::class)
  fun readPackedInt(stream: InputStream, numBytes: Int, isLittleEndian: Boolean): Int {
    var value = 0
    for (i in 0 until numBytes) {
      val b = stream.read()
      if (b == -1) {
        throw IOException("no more bytes")
      }
      value =
          if (isLittleEndian) {
            value or ((b and 0xFF) shl (i * 8))
          } else {
            (value shl 8) or (b and 0xFF)
          }
    }
    return value
  }
}
