/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.Closeable;
import java.io.InputStream;

/**
 * A 'pooled' byte-buffer abstraction. Represents an immutable sequence of bytes stored off the
 * java heap.
 */
public interface PooledByteBuffer extends Closeable {

  /**
   * Get the size of the byte buffer
   * @return the size of the byte buffer
   */
  int size();

  /**
   * Read byte at given offset
   * @param offset
   * @return byte at given offset
   */
  byte read(int offset);

  /**
   * Read consecutive bytes.
   *
   * @param offset the position in the PooledByteBuffer of the first byte to read
   * @param buffer the byte array where read bytes will be copied to
   * @param bufferOffset the position within the buffer of the first copied byte
   * @param length number of bytes to copy
   * @return number of bytes copied
   */
  void read(int offset, byte[] buffer, int bufferOffset, int length);

  /**
   * @return pointer to native memory backing this buffer
   */
  long getNativePtr();

  /**
   * Close this PooledByteBuffer and release all underlying resources
   */
  @Override
  void close();

  /**
   * Check if this instance has already been closed
   * @return true, if the instance has been closed
   */
  boolean isClosed();

  /**
   * Exception indicating that the PooledByteBuffer is closed
   */
  public static class ClosedException extends RuntimeException {
    public ClosedException() {
      super("Invalid bytebuf. Already closed");
    }
  }
}
