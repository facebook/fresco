/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Preconditions;

/**
 * Utility method for dealing with Streams.
 */
public class StreamUtil {

  /**
   * Efficiently fetch bytes from InputStream is by delegating to
   * getBytesFromStream(is, is.available())
   */
  public static byte[] getBytesFromStream(final InputStream is) throws IOException {
    return getBytesFromStream(is, is.available());
  }

  /**
   * Efficiently fetch the bytes from the InputStream, provided that caller can guess
   * exact numbers of bytes that can be read from inputStream. Avoids one extra byte[] allocation
   * that ByteStreams.toByteArray() performs.
   * @param hint - size of inputStream's content in bytes
   */
  public static byte[] getBytesFromStream(InputStream inputStream, int hint) throws IOException {
    // Subclass ByteArrayOutputStream to avoid an extra byte[] allocation and copy
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(hint) {
      @Override
      public byte[] toByteArray() {
        // Can only use the raw buffer directly if the size is equal to the array we have.
        // Otherwise we have no choice but to copy.
        if (count == buf.length) {
          return buf;
        } else {
          return super.toByteArray();
        }
      }
    };
    ByteStreams.copy(inputStream, byteOutput);
    return byteOutput.toByteArray();
  }

  /**
   * Skips exactly bytesCount bytes in inputStream unless end of stream is reached first.
   *
   * @param inputStream input stream to skip bytes from
   * @param bytesCount number of bytes to skip
   * @return number of skipped bytes
   * @throws IOException
   */
  public static long skip(final InputStream inputStream, final long bytesCount) throws IOException {
    Preconditions.checkNotNull(inputStream);
    Preconditions.checkArgument(bytesCount >= 0);

    long toSkip = bytesCount;
    while (toSkip > 0) {
      final long skipped = inputStream.skip(toSkip);
      if (skipped > 0) {
        toSkip -= skipped;
        continue;
      }

      if (inputStream.read() != -1) {
        toSkip--;
        continue;
      }
      return bytesCount - toSkip;
    }

    return bytesCount;
  }
}
