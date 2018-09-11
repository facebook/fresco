/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.util.Log;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.nativecode.ImagePipelineNativeLoader;
import java.io.Closeable;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/**
 * Wrapper around chunk of native memory.
 *
 * <p>This class uses JNI to obtain pointer to native memory and read/write data from/to it.
 *
 * <p>Native code used by this class is shipped as part of libimagepipeline.so @ThreadSafe
 */
@DoNotStrip
public class NativeMemoryChunk implements MemoryChunk, Closeable {
  private static final String TAG = "NativeMemoryChunk";

  static {
    ImagePipelineNativeLoader.load();
  }

  /**
   * Address of memory chunk wrapped by this NativeMemoryChunk
   */
  private final long mNativePtr;

  /**
   * size of the memory region
   */
  private final int mSize;

  /** flag indicating if this object was closed @GuardedBy("this") */
  private boolean mIsClosed;

  public NativeMemoryChunk(final int size) {
    Preconditions.checkArgument(size > 0);
    mSize = size;
    mNativePtr = nativeAllocate(mSize);
    mIsClosed = false;
  }

  @VisibleForTesting
  public NativeMemoryChunk() {
    mSize = 0;
    mNativePtr = 0;
    mIsClosed = true;
  }

  @Override
  public synchronized void close() {
    if (!mIsClosed) {
      mIsClosed = true;
      nativeFree(mNativePtr);
    }
  }

  @Override
  public synchronized boolean isClosed() {
    return mIsClosed;
  }

  @Override
  public int getSize() {
    return mSize;
  }

  @Override
  public synchronized int write(
      final int memoryOffset, final byte[] byteArray, final int byteArrayOffset, final int count) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkState(!isClosed());
    final int actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, mSize);
    MemoryChunkUtil.checkBounds(
        memoryOffset, byteArray.length, byteArrayOffset, actualCount, mSize);
    nativeCopyFromByteArray(mNativePtr + memoryOffset, byteArray, byteArrayOffset, actualCount);
    return actualCount;
  }

  @Override
  public synchronized int read(
      final int memoryOffset, final byte[] byteArray, final int byteArrayOffset, final int count) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkState(!isClosed());
    final int actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, mSize);
    MemoryChunkUtil.checkBounds(
        memoryOffset, byteArray.length, byteArrayOffset, actualCount, mSize);
    nativeCopyToByteArray(mNativePtr + memoryOffset, byteArray, byteArrayOffset, actualCount);
    return actualCount;
  }

  @Override
  public synchronized byte read(final int offset) {
    Preconditions.checkState(!isClosed());
    Preconditions.checkArgument(offset >= 0);
    Preconditions.checkArgument(offset < mSize);
    return nativeReadByte(mNativePtr + offset);
  }

  @Override
  public long getNativePtr() {
    return mNativePtr;
  }

  @Override
  @Nullable
  public ByteBuffer getByteBuffer() {
    return null;
  }

  @Override
  public long getUniqueId() {
    return mNativePtr;
  }

  @Override
  public void copy(
      final int offset, final MemoryChunk other, final int otherOffset, final int count) {
    Preconditions.checkNotNull(other);

    // This implementation acquires locks on this and other objects and then delegates to
    // doCopy which does actual copy. In order to avoid deadlocks we have to establish some linear
    // order on all NativeMemoryChunks and acquire locks according to this order. Fortunately
    // we can use the unique ids for that purpose. So we have to address 3 cases:

    // Case 1: other memory chunk == this memory chunk
    if (other.getUniqueId() == getUniqueId()) {
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
    if (other.getUniqueId() < getUniqueId()) {
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

  /**
   * This does actual copy. It should be called only when we hold locks on both this and other
   * objects
   */
  private void doCopy(
      final int offset, final MemoryChunk other, final int otherOffset, final int count) {
    if (!(other instanceof NativeMemoryChunk)) {
      throw new IllegalArgumentException("Cannot copy two incompatible MemoryChunks");
    }
    Preconditions.checkState(!isClosed());
    Preconditions.checkState(!other.isClosed());
    MemoryChunkUtil.checkBounds(offset, other.getSize(), otherOffset, count, mSize);
    nativeMemcpy(other.getNativePtr() + otherOffset, mNativePtr + offset, count);
  }

  /**
   * A finalizer, just in case. Just delegates to {@link #close()}
   *
   * @throws Throwable
   */
  @Override
  protected void finalize() throws Throwable {
    if (isClosed()) {
      return;
    }

    Log.w(
        TAG,
        "finalize: Chunk "
            + Integer.toHexString(System.identityHashCode(this))
            + " still active. ");
    // do the actual clearing
    try {
      close();
    } finally {
      super.finalize();
    }
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
