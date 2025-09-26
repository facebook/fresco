/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory

import java.io.Closeable
import java.nio.ByteBuffer

/**
 * A 'pooled' byte-buffer abstraction. Represents an immutable sequence of bytes stored off the java
 * heap.
 */
interface PooledByteBuffer : Closeable {
  /**
   * Get the size of the byte buffer
   *
   * @return the size of the byte buffer
   */
  fun size(): Int

  /**
   * Read byte at given offset
   *
   * @param offset
   * @return byte at given offset
   */
  fun read(offset: Int): Byte

  /**
   * Read consecutive bytes.
   *
   * @param offset the position in the PooledByteBuffer of the first byte to read
   * @param buffer the byte array where read bytes will be copied to
   * @param bufferOffset the position within the buffer of the first copied byte
   * @param length number of bytes to copy
   * @return number of bytes copied
   */
  fun read(offset: Int, buffer: ByteArray, bufferOffset: Int, length: Int): Int

  /**
   * Gets the pointer to native memory backing this buffer if present
   *
   * @return the pointer
   * @throws UnsupportedOperationException if the buffer does not have a pointer to memory
   */
  val nativePtr: Long

  /** Gets the underlying ByteBuffer backing this buffer if present, else null. */
  val byteBuffer: ByteBuffer?

  /** Close this PooledByteBuffer and release all underlying resources */
  override fun close()

  /**
   * Check if this instance has already been closed
   *
   * @return true, if the instance has been closed
   */
  val isClosed: Boolean

  /** Exception indicating that the PooledByteBuffer is closed */
  class ClosedException : RuntimeException("Invalid bytebuf. Already closed")
}
