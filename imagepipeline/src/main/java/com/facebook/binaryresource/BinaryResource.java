/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.binaryresource;

import java.io.IOException;
import java.io.InputStream;

/*
 * Interface representing a sequence of bytes that abstracts underlying source (e.g. file).
 *
 * <p>It represents a non-volatile resource whenever it exists and it can be read multiple times.
 * Since it is stream based, performing transformations like encryption can be done by a
 * simple wrapper, instead of writing the decrypted content of the original file into a new file.
 *
 * <p>Inspired partly by Guava's ByteSource class, but does not use its implementation.
 */
public interface BinaryResource {

  /**
   * Opens a new {@link InputStream} for reading from this source. This method should return a new,
   * independent stream each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned stream is closed.
   *
   * @throws IOException if an I/O error occurs in the process of opening the stream
   */
  InputStream openStream() throws IOException;

  /**
   * Reads the full contents of this byte source as a byte array.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
   */
   byte[] read() throws IOException;

  /**
   * Returns the size of this source in bytes. This may be a heavyweight
   * operation that will open a stream, read (or {@link InputStream#skip(long) skip}, if possible)
   * to the end of the stream and return the total number of bytes that were read.
   *
   * <p>For some sources, such as a file, this method may use a more efficient implementation. Note
   * that in such cases, it is <i>possible</i> that this method will return a different number of
   * bytes than would be returned by reading all of the bytes (for example, some special files may
   * return a size of 0 despite actually having content when read).
   *
   * <p>In either case, if this is a mutable source such as a file, the size it returns may not be
   * the same number of bytes a subsequent read would return.
   *
   * @throws IOException if an I/O error occurs in the process of reading the size of this source
   */
  long size();
}
