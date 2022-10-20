/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import java.nio.ByteBuffer

interface MemoryChunk {

  /**
   * This has to be called before we get rid of this object in order to release underlying memory
   */
  fun close()

  /**
   * Check if this chunk is already closed
   *
   * @return true, if this chunk has already been closed
   */
  fun isClosed(): Boolean

  /** Get the size of this memory chunk. Ignores if this chunk has been closed */
  val size: Int

  /**
   * Copy bytes from byte array to buffer.
   *
   * @param memoryOffset number of first byte to be written by copy operation
   * @param byteArray byte array to copy from
   * @param byteArrayOffset number of first byte in byteArray to copy
   * @param count number of bytes to copy
   * @return number of bytes written
   */
  fun write(memoryOffset: Int, byteArray: ByteArray, byteArrayOffset: Int, count: Int): Int

  /**
   * Copy bytes from memory to byte array.
   *
   * @param memoryOffset number of first byte to copy
   * @param byteArray byte array to copy to
   * @param byteArrayOffset number of first byte in byte array to be written
   * @param count number of bytes to copy
   * @return number of bytes read
   */
  fun read(memoryOffset: Int, byteArray: ByteArray, byteArrayOffset: Int, count: Int): Int

  /**
   * Read byte at given offset.
   *
   * @param offset The offset from which the byte will be read
   * @return byte at given offset
   */
  fun read(offset: Int): Byte

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
  fun copy(offset: Int, other: MemoryChunk, otherOffset: Int, count: Int)

  /**
   * Gets the pointer the native memory.
   *
   * @throws UnsupportedOperationException if the memory chunk is not in native memory
   */
  @get:Throws(UnsupportedOperationException::class) val nativePtr: Long

  /** Gets the ByteBuffer associated with the memory chunk if available, else null. */
  val byteBuffer: ByteBuffer?

  /** Gets the unique identifier associated with the memory chunk. */
  val uniqueId: Long
}
