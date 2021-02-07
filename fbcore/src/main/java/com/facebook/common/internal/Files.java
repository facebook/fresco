/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.common.internal;

import com.facebook.infer.annotation.Nullsafe;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides utility methods for working with files.
 *
 * <p>All method parameters must be non-null unless documented otherwise.
 *
 * @author Chris Nokleberg
 * @author Colin Decker
 * @since 1.0
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class Files {
  private Files() {}

  /**
   * Reads a file of the given expected size from the given input stream, if it will fit into a byte
   * array. This method handles the case where the file size changes between when the size is read
   * and when the contents are read from the stream.
   */
  static byte[] readFile(InputStream in, long expectedSize) throws IOException {
    if (expectedSize > Integer.MAX_VALUE) {
      throw new OutOfMemoryError(
          "file is too large to fit in a byte array: " + expectedSize + " bytes");
    }

    // some special files may return size 0 but have content, so read
    // the file normally in that case
    return expectedSize == 0
        ? ByteStreams.toByteArray(in)
        : ByteStreams.toByteArray(in, (int) expectedSize);
  }
  /**
   * Reads all bytes from a file into a byte array.
   *
   * @param file the file to read from
   * @return a byte array containing all the bytes from file
   * @throws IllegalArgumentException if the file is bigger than the largest possible byte array
   *     (2^31 - 1)
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(File file) throws IOException {
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);
      return readFile(in, in.getChannel().size());
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }
}
