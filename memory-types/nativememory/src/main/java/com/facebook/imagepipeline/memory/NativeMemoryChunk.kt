/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.DoNotStrip
import com.facebook.soloader.nativeloader.NativeLoader
import java.io.Closeable
import java.nio.ByteBuffer

/**
 * Wrapper around chunk of native memory.
 *
 * This class uses JNI to obtain pointer to native memory and read/write data from/to it.
 *
 * Native code used by this class is shipped as part of libimagepipeline.so @ThreadSafe
 */
@DoNotStrip
open class NativeMemoryChunk : MemoryChunk, Closeable {

  /** Address of memory chunk wrapped by this NativeMemoryChunk */
  private val mNativePtr: Long

  /** size of the memory region */
  private val mSize: Int

  /** flag indicating if this object was closed @GuardedBy("this") */
  private var _isClosed: Boolean

  constructor(size: Int) {
    require(size > 0)
    mSize = size
    mNativePtr = nativeAllocate(mSize)
    _isClosed = false
  }

  @VisibleForTesting
  constructor() {
    mSize = 0
    mNativePtr = 0
    _isClosed = true
  }

  override val size: Int
    get() = mSize

  override val nativePtr: Long
    get() = mNativePtr

  override val uniqueId: Long
    get() = mNativePtr

  @Synchronized
  override fun close() {
    if (!_isClosed) {
      _isClosed = true
      nativeFree(uniqueId)
    }
  }

  @Synchronized override fun isClosed(): Boolean = _isClosed

  @Synchronized
  override fun write(
      memoryOffset: Int,
      byteArray: ByteArray,
      byteArrayOffset: Int,
      count: Int,
  ): Int {
    checkNotNull(byteArray)
    check(!isClosed())
    val actualCount =
        MemoryChunkUtil.adjustByteCount(
            memoryOffset,
            count,
            size,
        )
    MemoryChunkUtil.checkBounds(
        memoryOffset,
        byteArray.size,
        byteArrayOffset,
        actualCount,
        size,
    )
    nativeCopyFromByteArray(uniqueId + memoryOffset, byteArray, byteArrayOffset, actualCount)
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
    val actualCount =
        MemoryChunkUtil.adjustByteCount(
            memoryOffset,
            count,
            size,
        )
    MemoryChunkUtil.checkBounds(
        memoryOffset,
        byteArray.size,
        byteArrayOffset,
        actualCount,
        size,
    )
    nativeCopyToByteArray(uniqueId + memoryOffset, byteArray, byteArrayOffset, actualCount)
    return actualCount
  }

  @Synchronized
  override fun read(offset: Int): Byte {
    check(!isClosed())
    require(offset >= 0)
    require(offset < size)
    return nativeReadByte(uniqueId + offset)
  }

  override val byteBuffer: ByteBuffer?
    get() = null

  override fun copy(
      offset: Int,
      other: MemoryChunk,
      otherOffset: Int,
      count: Int,
  ) {
    checkNotNull(other)

    // This implementation acquires locks on this and other objects and then delegates to
    // doCopy which does actual copy. In order to avoid deadlocks we have to establish some linear
    // order on all NativeMemoryChunks and acquire locks according to this order. Fortunately
    // we can use the unique ids for that purpose. So we have to address 3 cases:

    // Case 1: other memory chunk == this memory chunk
    if (other.uniqueId == uniqueId) {
      // we do not allow copying to the same address
      // lets log warning and not copy
      Log.w(
          TAG,
          ("Copying from NativeMemoryChunk ${Integer.toHexString(System.identityHashCode(this))} to NativeMemoryChunk ${Integer.toHexString(System.identityHashCode(other))} which share the same address ${java.lang.Long.toHexString(uniqueId)}"),
      )
      require(false)
    }

    // Case 2: other memory chunk < this memory chunk
    if (other.uniqueId < uniqueId) {
      synchronized(other) {
        synchronized(this) {
          doCopy(offset, other, otherOffset, count)
        }
      }
      return
    }

    // Case 3: other memory chunk > this memory chunk
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
    require(other is NativeMemoryChunk) { "Cannot copy two incompatible MemoryChunks" }
    check(!isClosed())
    check(!other.isClosed())
    MemoryChunkUtil.checkBounds(offset, other.size, otherOffset, count, size)
    nativeMemcpy(other.nativePtr + otherOffset, uniqueId + offset, count)
  }

  /**
   * A finalizer, just in case. Just delegates to [close()]
   *
   * @throws Throwable
   */
  // This is a valid use of finalize. No other mechanism is appropriate.
  @Throws(Throwable::class)
  protected open fun finalize() {
    if (isClosed()) {
      return
    }

    Log.w(
        TAG,
        ("finalize: Chunk ${Integer.toHexString(System.identityHashCode(this))} still active. "),
    )
    // do the actual clearing
    close()
  }

  companion object {
    private const val TAG = "NativeMemoryChunk"

    init {
      NativeLoader.loadLibrary("imagepipeline")
    }

    /** Delegate to one of native memory allocation function */
    @JvmStatic
    /** Delegate to one of native memory allocation function */
    @DoNotStrip
    private external fun nativeAllocate(size: Int): Long

    /** Delegate to appropriate memory releasing function */
    @JvmStatic
    /** Delegate to appropriate memory releasing function */
    @DoNotStrip
    private external fun nativeFree(address: Long)

    /** Copy count bytes pointed by mNativePtr to array, starting at position offset */
    @JvmStatic
    /** Copy count bytes pointed by mNativePtr to array, starting at position offset */
    @DoNotStrip
    private external fun nativeCopyToByteArray(
        address: Long,
        array: ByteArray,
        offset: Int,
        count: Int,
    )

    /** Copy count bytes from byte array to native memory pointed by mNativePtr. */
    @JvmStatic
    /** Copy count bytes from byte array to native memory pointed by mNativePtr. */
    @DoNotStrip
    private external fun nativeCopyFromByteArray(
        address: Long,
        array: ByteArray,
        offset: Int,
        count: Int,
    )

    /** Copy count bytes from memory pointed by fromPtr to memory pointed by toPtr */
    @JvmStatic
    /** Copy count bytes from memory pointed by fromPtr to memory pointed by toPtr */
    @DoNotStrip
    private external fun nativeMemcpy(toPtr: Long, fromPtr: Long, count: Int)

    /**
     * Read single byte from given address
     *
     * @param fromPtr address to read byte from
     */
    @JvmStatic
    /**
     * Read single byte from given address
     *
     * @param fromPtr address to read byte from
     */
    @DoNotStrip
    private external fun nativeReadByte(fromPtr: Long): Byte
  }
}
