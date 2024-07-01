/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import androidx.annotation.VisibleForTesting
import com.facebook.common.memory.PooledByteBufferOutputStream
import com.facebook.common.references.CloseableReference
import java.io.IOException
import javax.annotation.concurrent.NotThreadSafe

/** An implementation of [PooledByteBufferOutputStream] that produces a [MemoryPooledByteBuffer] */
@NotThreadSafe
class MemoryPooledByteBufferOutputStream
@JvmOverloads
constructor(pool: MemoryChunkPool, initialCapacity: Int = pool.minBufferSize) :
    PooledByteBufferOutputStream() {

  private val pool // the pool to allocate memory chunks from
  : MemoryChunkPool
  private var bufRef // the current chunk that we're writing to
  : CloseableReference<MemoryChunk>?
  private var count // number of bytes 'used' in the current chunk
  : Int

  /**
   * Construct a new instance of this OutputStream
   *
   * @param pool the pool to use
   */
  init {
    check(initialCapacity > 0)
    this.pool = checkNotNull(pool)
    count = 0
    bufRef = CloseableReference.of(this.pool[initialCapacity], this.pool)
  }

  /**
   * Gets a PooledByteBuffer from the current contents. If the stream has already been closed, then
   * an InvalidStreamException is thrown.
   *
   * @return a PooledByteBuffer instance for the contents of the stream
   * @throws InvalidStreamException if the stream is invalid
   */
  override fun toByteBuffer(): MemoryPooledByteBuffer {
    ensureValid()
    return MemoryPooledByteBuffer(checkNotNull(bufRef), count)
  }

  /**
   * Returns the total number of bytes written to this stream so far.
   *
   * @return the number of bytes written to this stream.
   */
  override fun size(): Int = count

  /**
   * Write one byte to the underlying stream. The underlying stream MUST be valid
   *
   * @param oneByte the one byte to write
   * @throws InvalidStreamException if the stream is invalid
   * @throws IOException in case of an I/O error during the write
   */
  @Throws(IOException::class)
  override fun write(oneByte: Int) {
    val buf = ByteArray(1)
    buf[0] = oneByte.toByte()
    this.write(buf)
  }

  /**
   * Writes `count` bytes from the byte array `buffer` starting at position `offset` to this stream.
   * The underlying stream MUST be valid
   *
   * @param buffer the source buffer to read from
   * @param offset the start position in `buffer` from where to get bytes.
   * @param count the number of bytes from `buffer` to write to this stream.
   * @throws IOException if an error occurs while writing to this stream.
   * @throws IndexOutOfBoundsException if `offset < 0` or `count < 0`, or if `offset + count` is
   *   bigger than the length of `buffer`.
   * @throws InvalidStreamException if the stream is invalid
   */
  @Throws(IOException::class)
  override fun write(buffer: ByteArray, offset: Int, count: Int) {
    if (offset < 0 || count < 0 || offset + count > buffer.size) {
      throw ArrayIndexOutOfBoundsException(
          "length=${buffer.size}; regionStart=${offset}; regionLength=${count}")
    }
    ensureValid()
    realloc(this.count + count)
    checkNotNull(bufRef).get().write(this.count, buffer, offset, count)
    this.count += count
  }

  /**
   * Closes the stream. Owned resources are released back to the pool. It is not allowed to call
   * toByteBuffer after call to this method.
   *
   * @throws IOException
   */
  override fun close() {
    CloseableReference.closeSafely(bufRef)
    bufRef = null
    count = -1
    super.close()
  }

  /**
   * Reallocate the local buffer to hold the new length specified. Also copy over existing data to
   * this new buffer
   *
   * @param newLength new length of buffer
   * @throws InvalidStreamException if the stream is invalid
   * @throws BasePool.SizeTooLargeException if the allocation from the pool fails
   */
  @VisibleForTesting
  fun realloc(newLength: Int) {
    ensureValid()
    checkNotNull(bufRef)
    /* Can the buffer handle @i more bytes, if not expand it */ if (newLength <=
        bufRef!!.get().size) {
      return
    }
    val newbuf = this.pool[newLength]
    checkNotNull(bufRef)
    bufRef!!.get().copy(0, newbuf, 0, count)
    bufRef!!.close()
    bufRef = CloseableReference.of(newbuf, this.pool)
  }

  /**
   * Ensure that the current stream is valid, that is underlying closeable reference is not null and
   * is valid
   *
   * @throws InvalidStreamException if the stream is invalid
   */
  private fun ensureValid() {
    if (!CloseableReference.isValid(bufRef)) {
      throw InvalidStreamException()
    }
  }

  /** An exception indicating that this stream is no longer valid */
  class InvalidStreamException : RuntimeException("OutputStream no longer valid")

  /**
   * Construct a new instance of this output stream with this initial capacity It is not an error to
   * have this initial capacity be inaccurate. If the actual contents end up being larger than the
   * initialCapacity, then we will reallocate memory if needed. If the actual contents are smaller,
   * then we'll end up wasting some memory
   *
   * @param pool the pool to use
   * @param initialCapacity initial capacity to allocate for this stream
   */
}
