/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.Preconditions
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBuffer.ClosedException
import com.facebook.common.references.CloseableReference
import java.nio.ByteBuffer
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe

/** An implementation of [PooledByteBuffer] that uses ([MemoryChunk]) to store data */
@ThreadSafe
class MemoryPooledByteBuffer(bufRef: CloseableReference<MemoryChunk>, size: Int) :
    PooledByteBuffer {
  private val mSize: Int

  @get:VisibleForTesting
  @get:GuardedBy("this")
  @GuardedBy("this")
  @VisibleForTesting
  var closeableReference: CloseableReference<MemoryChunk?>?

  init {
    Preconditions.checkArgument(size >= 0 && size <= bufRef.get().size)
    this.closeableReference = bufRef.clone()
    mSize = size
  }

  /**
   * Gets the size of the ByteBuffer if it is valid. Otherwise, an exception is raised
   *
   * @return the size of the ByteBuffer if it is not closed.
   * @throws [ClosedException]
   */
  @Synchronized
  override fun size(): Int {
    ensureValid()
    return mSize
  }

  @Synchronized
  override fun read(offset: Int): Byte {
    ensureValid()
    Preconditions.checkArgument(offset >= 0)
    Preconditions.checkArgument(offset < mSize)
    Preconditions.checkNotNull<CloseableReference<MemoryChunk?>?>(this.closeableReference)
    return closeableReference!!.get().read(offset)
  }

  @Synchronized
  override fun read(offset: Int, buffer: ByteArray, bufferOffset: Int, length: Int): Int {
    ensureValid()
    // We need to make sure that PooledByteBuffer's length is preserved.
    // Al the other bounds checks will be performed by NativeMemoryChunk.read method.
    Preconditions.checkArgument(offset + length <= mSize)
    Preconditions.checkNotNull<CloseableReference<MemoryChunk?>?>(this.closeableReference)
    return closeableReference!!.get().read(offset, buffer, bufferOffset, length)
  }

  @get:Throws(UnsupportedOperationException::class)
  @get:Synchronized
  override val nativePtr: Long
    get() {
      ensureValid()
      Preconditions.checkNotNull<CloseableReference<MemoryChunk?>?>(this.closeableReference)
      return closeableReference!!.get().nativePtr
    }

  @get:Synchronized
  override val byteBuffer: ByteBuffer?
    get() {
      Preconditions.checkNotNull<CloseableReference<MemoryChunk?>?>(this.closeableReference)
      return closeableReference!!.get().byteBuffer
    }

  @get:Synchronized
  override val isClosed: Boolean
    get() = !CloseableReference.isValid(this.closeableReference)

  /**
   * Closes this instance, and releases the underlying buffer to the pool. Once the ByteBuffer has
   * been closed, subsequent operations (especially `getStream()` will fail. Note: It is not an
   * error to close an already closed ByteBuffer
   */
  @Synchronized
  override fun close() {
    CloseableReference.closeSafely(this.closeableReference)
    this.closeableReference = null
  }

  /**
   * Validates that the ByteBuffer instance is valid (aka not closed). If it is closed, then we
   * raise a ClosedException This doesn't really need to be synchronized, but lint won't shut up
   * otherwise
   *
   * @throws ClosedException
   */
  @Synchronized
  fun ensureValid() {
    if (isClosed) {
      throw PooledByteBuffer.ClosedException()
    }
  }
}
