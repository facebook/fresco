/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;

public interface MemoryChunk {
  /**
   * This has to be called before we get rid of this object in order to release underlying memory
   */
  void close();

  /**
   * Check if this chunk is already closed
   *
   * @return true, if this chunk has already been closed
   */
  boolean isClosed();

  /** Get the size of this memory chunk. Ignores if this chunk has been closed */
  int getSize();

  /**
   * Copy bytes from byte array to buffer.
   *
   * @param memoryOffset number of first byte to be written by copy operation
   * @param byteArray byte array to copy from
   * @param byteArrayOffset number of first byte in byteArray to copy
   * @param count number of bytes to copy
   * @return number of bytes written
   */
  int write(
      final int memoryOffset, final byte[] byteArray, final int byteArrayOffset, final int count);
  /**
   * Copy bytes from memory to byte array.
   *
   * @param memoryOffset number of first byte to copy
   * @param byteArray byte array to copy to
   * @param byteArrayOffset number of first byte in byte array to be written
   * @param count number of bytes to copy
   * @return number of bytes read
   */
  int read(
      final int memoryOffset, final byte[] byteArray, final int byteArrayOffset, final int count);

  /**
   * Read byte at given offset.
   *
   * @param offset The offset from which the byte will be read
   * @return byte at given offset
   */
  byte read(final int offset);

  /**
   * Copy bytes from buffer memory wrapped by this MemoryChunk instance to buffer memory wrapped by
   * another MemoryChunk. The two MemoryChunks should have the same type.
   *
   * @param offset number of first byte to copy
   * @param other other MemoryChunk to copy to
   * @param otherOffset number of first byte to write to
   * @param count number of bytes to copy
   * @throws IllegalArgumentException if the memory chunks don't have the same type
   */
  void copy(final int offset, final MemoryChunk other, final int otherOffset, final int count);

  /**
   * Gets the pointer the native memory.
   *
   * @throws UnsupportedOperationException if the memory chunk is not in native memory
   */
  long getNativePtr() throws UnsupportedOperationException;

  /** Gets the ByteBuffer associated with the memory chunk if available, else null. */
  @Nullable
  ByteBuffer getByteBuffer();

  /** Gets the unique identifier associated with the memory chunk. */
  long getUniqueId();
}
