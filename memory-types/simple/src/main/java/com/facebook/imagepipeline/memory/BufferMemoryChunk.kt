/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.util.Log
import java.io.Closeable
import java.nio.ByteBuffer

/**
 * Wrapper around chunk using a direct ByteBuffer in native memory. A direct ByteBuffer is composed
 * of a Java [ByteBuffer] object allocated on the Java heap and the underlying buffer which is in
 * native memory.
 *
 * The buffer in native memory will be released when the Java object gets garbage collected.
 */
class BufferMemoryChunk(
    /** Size of the ByteBuffer */
    override val size: Int
) : MemoryChunk, Closeable {

  /** Internal representation of the chunk */
  @get:Synchronized
  override var byteBuffer: ByteBuffer?
    private set

  /** Unique identifier of the chunk */
  override val uniqueId: Long

  @Synchronized
  override fun close() {
    byteBuffer = null
  }

  @Synchronized override fun isClosed(): Boolean = byteBuffer == null

  @Synchronized
  override fun write(
      memoryOffset: Int,
      byteArray: ByteArray,
      byteArrayOffset: Int,
      count: Int,
  ): Int {
    checkNotNull(byteArray)
    check(!isClosed())
    checkNotNull(byteBuffer)
    val actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, size)
    MemoryChunkUtil.checkBounds(memoryOffset, byteArray.size, byteArrayOffset, actualCount, size)
    byteBuffer!!.position(memoryOffset)
    byteBuffer!!.put(byteArray, byteArrayOffset, actualCount)
    return actualCount
  }

  @Synchronized
  override fun read(
      memoryOffset: Int,
      byteArray: ByteArray,
      byteArrayOffset: Int,
      count: Int,
  ): Int {
    checkNotNull(byteArray)
    check(!isClosed())
    checkNotNull(byteBuffer)
    val actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, size)
    MemoryChunkUtil.checkBounds(memoryOffset, byteArray.size, byteArrayOffset, actualCount, size)
    byteBuffer!!.position(memoryOffset)
    byteBuffer!![byteArray, byteArrayOffset, actualCount]
    return actualCount
  }

  @Synchronized
  override fun read(offset: Int): Byte {
    check(!isClosed())
    require(offset >= 0)
    require(offset < size)
    checkNotNull(byteBuffer)
    return byteBuffer!![offset]
  }

  override fun copy(offset: Int, other: MemoryChunk, otherOffset: Int, count: Int) {
    checkNotNull(other)

    // This implementation acquires locks on this and other objects and then delegates to
    // doCopy which does actual copy. In order to avoid deadlocks we have to establish some linear
    // order on all BufferMemoryChunks and acquire locks according to this order. In order
    // to do that, we use unique ids.
    // So we have to address 3 cases:

    // Case 1: other buffer equals this buffer, id comparison
    if (other.uniqueId == uniqueId) {
      // we do not allow copying to the same address
      // lets log warning and not copy
      Log.w(
          TAG,
          ("Copying from BufferMemoryChunk ${java.lang.Long.toHexString(uniqueId)} to BufferMemoryChunk ${java.lang.Long.toHexString(other.uniqueId)} which are the same "),
      )
      require(false)
    }

    // Case 2: Other memory chunk id < this memory chunk id
    if (other.uniqueId < uniqueId) {
      synchronized(other) { synchronized(this) { doCopy(offset, other, otherOffset, count) } }
      return
    }

    // Case 3: Other memory chunk id > this memory chunk id
    synchronized(this) { synchronized(other) { doCopy(offset, other, otherOffset, count) } }
  }

  override val nativePtr: Long
    get() {
      throw UnsupportedOperationException("Cannot get the pointer of a BufferMemoryChunk")
    }

  init {
    byteBuffer = ByteBuffer.allocateDirect(size)
    this.uniqueId = System.identityHashCode(this).toLong()
  }

  /**
   * This does actual copy. It should be called only when we hold locks on both this and other
   * objects
   */
  private fun doCopy(offset: Int, other: MemoryChunk, otherOffset: Int, count: Int) {
    require(other is BufferMemoryChunk) { "Cannot copy two incompatible MemoryChunks" }
    check(!isClosed())
    check(!other.isClosed())
    checkNotNull(byteBuffer)
    MemoryChunkUtil.checkBounds(offset, other.size, otherOffset, count, size)
    byteBuffer!!.position(offset)
    val otherByteBuffer = checkNotNull(other.byteBuffer)
    otherByteBuffer.position(otherOffset)
    // Recover the necessary part to be copied as a byte array.
    // This requires a copy, for now there is not a more efficient alternative.
    val b = ByteArray(count)
    byteBuffer!![b, 0, count]
    otherByteBuffer.put(b, 0, count)
  }

  companion object {
    private const val TAG = "BufferMemoryChunk"
  }
}
