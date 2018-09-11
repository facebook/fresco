/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Util for processing Stream.
 */
class StreamProcessor {

  /**
   *  Consumes up to 4 bytes and returns them as int (taking into account endianess).
   *  Throws exception if specified number of bytes cannot be consumed.
   *  @param is the input stream to read bytes from
   *  @param numBytes the number of bytes to read
   *  @param isLittleEndian whether the bytes should be interpreted in little or big endian format
   *  @return packed int read from input stream and constructed according to endianness
   */
  public static int readPackedInt(InputStream is, int numBytes, boolean isLittleEndian)
      throws IOException {
    int value = 0;
    for (int i = 0; i < numBytes; i++) {
      int b = is.read();
      if (b == -1) {
        throw new IOException("no more bytes");
      }
      if (isLittleEndian) {
        value |= (b & 0xFF) << (i * 8);
      } else {
        value = (value << 8) | (b & 0xFF);
      }
    }
    return value;
  }

}
