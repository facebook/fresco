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

import android.util.Log;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.soloader.SoLoaderShim;


/**
 * Wrapper around chunk of native memory.
 *
 * <p> This class uses JNI to obtain pointer to native memory and read/write data from/to it.
 *
 * <p> Native code used by this class is shipped as part of libimagepipeline_memory.so
 *
 * @ThreadSafe
 */
@DoNotStrip
public class NativeMemoryChunk implements Closeable {
  private static final String TAG = "NativeMemoryChunk";

  static {
    SoLoaderShim.loadLibrary("memchunk");
  }

  /**
   * Address of memory chunk wrapped by this NativeMemoryChunk
   */
  private final long mNativePtr;

  /**
   * size of the memory region
   */
  private final int mSize;

  /**
   * flag indicating if this object was closed
   * @GuardedBy("this")
   */
  private boolean mClosed;

  public NativeMemoryChunk(final int size) {
    Preconditions.checkArgument(size > 0);
    mSize = size;
    mNativePtr = nativeAllocate(mSize);
    mClosed = false;
  }

  @VisibleForTesting
  public NativeMemoryChunk() {
    mSize = 0;
    mNativePtr = 0;
    mClosed = true;
  }

  /**
   * This has to be called before we get rid of this object in order to release underlying memory
   */
  public synchronized void close() {
    if (!mClosed) {
      mClosed = true;
      nativeFree(mNativePtr);
    }
  }

  /**
   * Is this chunk already closed (aka freed) ?
   * @return true, if this chunk has already been closed
   */
  public synchronized boolean isClosed() {
    return mClosed;
  }

  /**
   * Get the size of this memory chunk.
   * Ignores if this chunk has been closed
   */
  public int getSize() {
    return mSize;
  }

  /**
   * Copy bytes from byte array to native memory.
   * @param nativeMemoryOffset number of first byte to be written by copy operation
   * @param byteArray byte array to copy from
   * @param byteArrayOffset number of first byte in byteArray to copy
   * @param count number of bytes to copy
   * @return number of bytes written
   */
  public synchronized int write(
      int nativeMemoryOffset,
      final byte[] byteArray,
      int byteArrayOffset,
      int count) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkState(!isClosed());
    final int actualCount = adjustByteCount(nativeMemoryOffset, count);
    checkBounds(nativeMemoryOffset, byteArray.length, byteArrayOffset, actualCount);
    nativeCopyFromByteArray(
        mNativePtr + nativeMemoryOffset,
        byteArray,
        byteArrayOffset,
        actualCount);
    return actualCount;
  }

  /**
   * Copy bytes from native memory to byte array.
   * @param nativeMemoryOffset number of first byte to copy
   * @param byteArray byte array to copy to
   * @param byteArrayOffset number of first byte in byte array to be written
   * @param count number of bytes to copy
   * @return number of bytes read
   */
  public synchronized int read(
      final int nativeMemoryOffset,
      final byte[] byteArray,
      final int byteArrayOffset,
      final int count) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkState(!isClosed());
    final int actualCount = adjustByteCount(nativeMemoryOffset, count);
    checkBounds(nativeMemoryOffset, byteArray.length, byteArrayOffset, actualCount);
    nativeCopyToByteArray(mNativePtr + nativeMemoryOffset, byteArray, byteArrayOffset, actualCount);
    return actualCount;
  }

  /**
   * Read byte at given offset.
   * @param offset
   * @return byte at given offset
   */
  public synchronized byte read(int offset) {
    Preconditions.checkState(!isClosed());
    Preconditions.checkArgument(offset >= 0);
    Preconditions.checkArgument(offset < mSize);
    return nativeReadByte(mNativePtr + offset);
  }

  /**
   * Copy bytes from native memory wrapped by this NativeMemoryChunk instance to
   * native memory wrapped by other NativeMemoryChunk
   * @param offset number of first byte to copy
   * @param other other NativeMemoryChunk to copy to
   * @param otherOffset number of first byte to write to
   * @param count number of bytes to copy
   */
  public void copy(
      final int offset,
      final NativeMemoryChunk other,
      final int otherOffset,
      final int count) {
    Preconditions.checkNotNull(other);

    // This implementation acquires locks on this and other objects and then delegates to
    // doCopy which does actual copy. In order to avoid deadlocks we have to establish some linear
    // order on all NativeMemoryChunks and acquire locks according to this order. Fortunately
    // we can use mNativePtr for that purpose. So we have to address 3 cases:

    // Case 1: other memory chunk == this memory chunk
    if (other.mNativePtr == mNativePtr) {
      // we do not allow copying to the same address
      // lets log warning and not copy
      Log.w(
          TAG,
          "Copying from NativeMemoryChunk " +
              Integer.toHexString(System.identityHashCode(this)) +
              " to NativeMemoryChunk " +
              Integer.toHexString(System.identityHashCode(other)) +
              " which share the same address " +
              Long.toHexString(mNativePtr));
      Preconditions.checkArgument(false);
    }

    // Case 2: other memory chunk < this memory chunk
    if (other.mNativePtr < mNativePtr) {
      synchronized (other) {
        synchronized (this) {
          doCopy(offset, other, otherOffset, count);
        }
      }
      return;
    }

    // Case 3: other memory chunk > this memory chunk
    synchronized (this) {
      synchronized (other) {
        doCopy(offset, other, otherOffset, count);
      }
    }
  }

  public long getNativePtr() {
    return mNativePtr;
  }

  /**
   * This does actual copy. It should be called only when we hold locks on both this and
   * other objects
   */
  private void doCopy(
      final int offset,
      final NativeMemoryChunk other,
      final int otherOffset,
      final int count) {
    Preconditions.checkState(!isClosed());
    Preconditions.checkState(!other.isClosed());
    checkBounds(offset, other.mSize, otherOffset, count);
    nativeMemcpy(other.mNativePtr + otherOffset, mNativePtr + offset, count);
  }

  /**
   * A finalizer, just in case. Just delegates to {@link #close()}
   * @throws Throwable
   */
  @Override
  protected void finalize() throws Throwable {
    if (isClosed()) {
      return;
    }

    Log.w(
        TAG,
        "finalize: Chunk " +
            Integer.toHexString(System.identityHashCode(this)) +
            " still active. Underlying address = " +
            Long.toHexString(mNativePtr));
    // do the actual clearing
    try {
      close();
    } finally {
      super.finalize();
    }
  }

  /**
   * Computes number of bytes that can be safely read/written starting at given offset, but no more
   * than count.
   */
  private int adjustByteCount(final int offset, final int count) {
    final int available = Math.max(0, mSize - offset);
    return Math.min(available, count);
  }

  /**
   * Check that copy/read/write operation won't access memory it should not
   */
  private void checkBounds(
      final int myOffset,
      final int otherLength,
      final int otherOffset,
      final int count) {
    Preconditions.checkArgument(count >= 0);
    Preconditions.checkArgument(myOffset >= 0);
    Preconditions.checkArgument(otherOffset >= 0);
    Preconditions.checkArgument(myOffset + count <= mSize);
    Preconditions.checkArgument(otherOffset + count <= otherLength);
  }

  /**
   * Delegate to one of native memory allocation function
   */
  @DoNotStrip
  private static native long nativeAllocate(int size);

  /**
   * Delegate to appropriate memory releasing function
   */
  @DoNotStrip
  private static native void nativeFree(long address);

  /**
   * Copy count bytes pointed by mNativePtr to array, starting at position offset
   */
  @DoNotStrip
  private static native void nativeCopyToByteArray(
      long address,
      byte[] array,
      int offset,
      int count);

  /**
   * Copy count bytes from byte array to native memory pointed by mNativePtr.
   */
  @DoNotStrip
  private static native void nativeCopyFromByteArray(
      long address,
      byte[] array,
      int offset,
      int count);

  /**
   * Copy count bytes from memory pointed by fromPtr to memory pointed by toPtr
   */
  @DoNotStrip
  private static native void nativeMemcpy(
      long toPtr,
      long fromPtr,
      int count);

  /**
   * Read single byte from given address
   * @param fromPtr address to read byte from
   */
  @DoNotStrip
  private static native byte nativeReadByte(long fromPtr);
}
