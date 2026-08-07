/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.annotation.TargetApi
import android.os.SharedMemory
import android.system.ErrnoException
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.io.Closeable
import java.nio.ByteBuffer

/** Wrapper around chunk of ashmem memory. */
@TargetApi(27)
open class AshmemMemoryChunk : MemoryChunk, Closeable {

  private var sharedMemory: SharedMemory? = null
  final override var byteBuffer: ByteBuffer? = null
    private set

  /** Unique identifier of the chunk */
  final override val uniqueId: Long

  constructor(size: Int) {
    require(size > 0)
    try {
      sharedMemory = SharedMemory.create(TAG, size)
      byteBuffer = sharedMemory!!.mapReadWrite()
    } catch (e: ErrnoException) {
      throw RuntimeException("Fail to create AshmemMemory", e)
    }
    this.uniqueId = System.identityHashCode(this).toLong()
  }

  @VisibleForTesting
  constructor() {
    sharedMemory = null
    byteBuffer = null
    this.uniqueId = System.identityHashCode(this).toLong()
  }

  @Synchronized
  override fun close() {
    if (!isClosed()) {
      sharedMemory?.close()
      if (byteBuffer != null) {
        SharedMemory.unmap(byteBuffer!!)
      }
      byteBuffer = null
      sharedMemory = null
    }
  }

  @Synchronized override fun isClosed(): Boolean = byteBuffer == null || sharedMemory == null

  override val size: Int
    get() {
      checkNotNull(sharedMemory)
      return sharedMemory!!.size
    }

  @Synchronized
  override fun write(
      memoryOffset: Int,
      byteArray: ByteArray,
      byteArrayOffset: Int,
      count: Int,
  ): Int {
    checkNotNull(byteArray)
    checkNotNull(byteBuffer)
    val actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, size)
    MemoryChunkUtil.checkBounds(
        memoryOffset,
        byteArray.size,
        byteArrayOffset,
        actualCount,
        size,
    )
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
    checkNotNull(byteBuffer)
    val actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, size)
    MemoryChunkUtil.checkBounds(
        memoryOffset,
        byteArray.size,
        byteArrayOffset,
        actualCount,
        size,
    )
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

  override val nativePtr: Long
    get() {
      throw UnsupportedOperationException("Cannot get the pointer of an  AshmemMemoryChunk")
    }

  override fun copy(
      offset: Int,
      other: MemoryChunk,
      otherOffset: Int,
      count: Int,
  ) {
    checkNotNull(other)

    // This implementation acquires locks on this and other objects and then delegates to
    // doCopy which does actual copy. In order to avoid deadlocks we have to establish some linear
    // order on all AshmemMemoryChunks and acquire locks according to this order. In order
    // to do that, we use unique ids.
    // So we have to address 3 cases:

    // Case 1: other buffer equals this buffer, id comparison
    if (other.uniqueId == uniqueId) {
      // we do not allow copying to the same address
      // lets log warning and not copy
      Log.w(
          TAG,
          ("Copying from AshmemMemoryChunk ${java.lang.Long.toHexString(uniqueId)} to AshmemMemoryChunk ${java.lang.Long.toHexString(other.uniqueId)} which are the same "),
      )
      require(false)
    }

    // Case 2: Other memory chunk id < this memory chunk id
    if (other.uniqueId < uniqueId) {
      synchronized(other) {
        synchronized(this) {
          doCopy(offset, other, otherOffset, count)
        }
      }
      return
    }

    // Case 3: Other memory chunk id > this memory chunk id
    synchronized(this) {
      synchronized(other) {
        doCopy(offset, other, otherOffset, count)
      }
    }
  }

  /**
   * This does actual copy. It should be called only when we hold locks on both this and other
   * objects
   */
  private fun doCopy(
      offset: Int,
      other: MemoryChunk,
      otherOffset: Int,
      count: Int,
  ) {
    require(other is AshmemMemoryChunk) { "Cannot copy two incompatible MemoryChunks" }
    check(!isClosed())
    check(!other.isClosed())
    checkNotNull(byteBuffer)
    checkNotNull(other.byteBuffer)
    MemoryChunkUtil.checkBounds(offset, other.size, otherOffset, count, size)
    byteBuffer!!.position(offset)
    // ByteBuffer can't be null at this point
    other.byteBuffer!!.position(otherOffset)
    // Recover the necessary part to be copied as a byte array.
    // This requires a copy, for now there is not a more efficient alternative.
    val b = ByteArray(count)
    byteBuffer!![b, 0, count]
    other.byteBuffer!!.put(b, 0, count)
  }

  companion object {
    private const val TAG = "AshmemMemoryChunk"
  }
}
