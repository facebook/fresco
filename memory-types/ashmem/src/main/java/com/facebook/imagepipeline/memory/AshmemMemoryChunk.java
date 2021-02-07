/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import android.annotation.TargetApi;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.Preconditions;
import java.io.Closeable;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/** Wrapper around chunk of ashmem memory. */
@TargetApi(27)
public class AshmemMemoryChunk implements MemoryChunk, Closeable {
  private static final String TAG = "AshmemMemoryChunk";

  private @Nullable SharedMemory mSharedMemory;
  private @Nullable ByteBuffer mByteBuffer;

  /** Unique identifier of the chunk */
  private final long mId;

  public AshmemMemoryChunk(final int size) {
    Preconditions.checkArgument(size > 0);
    try {
      mSharedMemory = SharedMemory.create(TAG, size);
      mByteBuffer = mSharedMemory.mapReadWrite();
    } catch (ErrnoException e) {
      throw new RuntimeException("Fail to create AshmemMemory", e);
    }
    mId = System.identityHashCode(this);
  }

  @VisibleForTesting
  public AshmemMemoryChunk() {
    mSharedMemory = null;
    mByteBuffer = null;
    mId = System.identityHashCode(this);
  }

  @Override
  public synchronized void close() {
    if (!isClosed()) {
      mSharedMemory.unmap(mByteBuffer);
      mSharedMemory.close();
      mByteBuffer = null;
      mSharedMemory = null;
    }
  }

  @Override
  public synchronized boolean isClosed() {
    return mByteBuffer == null || mSharedMemory == null;
  }

  @Override
  public int getSize() {
    Preconditions.checkState(!isClosed());
    return mSharedMemory.getSize();
  }

  @Override
  public synchronized int write(
      final int memoryOffset, final byte[] byteArray, final int byteArrayOffset, final int count) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkState(!isClosed());
    final int actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, getSize());
    MemoryChunkUtil.checkBounds(
        memoryOffset, byteArray.length, byteArrayOffset, actualCount, getSize());
    mByteBuffer.position(memoryOffset);
    mByteBuffer.put(byteArray, byteArrayOffset, actualCount);
    return actualCount;
  }

  @Override
  public synchronized int read(
      final int memoryOffset, final byte[] byteArray, final int byteArrayOffset, final int count) {
    Preconditions.checkNotNull(byteArray);
    Preconditions.checkState(!isClosed());
    final int actualCount = MemoryChunkUtil.adjustByteCount(memoryOffset, count, getSize());
    MemoryChunkUtil.checkBounds(
        memoryOffset, byteArray.length, byteArrayOffset, actualCount, getSize());
    mByteBuffer.position(memoryOffset);
    mByteBuffer.get(byteArray, byteArrayOffset, actualCount);
    return actualCount;
  }

  @Override
  public synchronized byte read(final int offset) {
    Preconditions.checkState(!isClosed());
    Preconditions.checkArgument(offset >= 0);
    Preconditions.checkArgument(offset < getSize());
    return mByteBuffer.get(offset);
  }

  @Override
  public long getNativePtr() {
    throw new UnsupportedOperationException("Cannot get the pointer of an  AshmemMemoryChunk");
  }

  @Override
  @Nullable
  public ByteBuffer getByteBuffer() {
    return mByteBuffer;
  }

  @Override
  public long getUniqueId() {
    return mId;
  }

  @Override
  public void copy(
      final int offset, final MemoryChunk other, final int otherOffset, final int count) {
    Preconditions.checkNotNull(other);
    // This implementation acquires locks on this and other objects and then delegates to
    // doCopy which does actual copy. In order to avoid deadlocks we have to establish some linear
    // order on all AshmemMemoryChunks and acquire locks according to this order. In order
    // to do that, we use unique ids.
    // So we have to address 3 cases:

    // Case 1: other buffer equals this buffer, id comparison
    if (other.getUniqueId() == getUniqueId()) {
      // we do not allow copying to the same address
      // lets log warning and not copy
      Log.w(
          TAG,
          "Copying from AshmemMemoryChunk "
              + Long.toHexString(getUniqueId())
              + " to AshmemMemoryChunk "
              + Long.toHexString(other.getUniqueId())
              + " which are the same ");
      Preconditions.checkArgument(false);
    }

    // Case 2: Other memory chunk id < this memory chunk id
    if (other.getUniqueId() < getUniqueId()) {
      synchronized (other) {
        synchronized (this) {
          doCopy(offset, other, otherOffset, count);
        }
      }
      return;
    }

    // Case 3: Other memory chunk id > this memory chunk id
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
    if (!(other instanceof AshmemMemoryChunk)) {
      throw new IllegalArgumentException("Cannot copy two incompatible MemoryChunks");
    }
    Preconditions.checkState(!isClosed());
    Preconditions.checkState(!other.isClosed());
    MemoryChunkUtil.checkBounds(offset, other.getSize(), otherOffset, count, getSize());
    mByteBuffer.position(offset);
    // ByteBuffer can't be null at this point
    other.getByteBuffer().position(otherOffset);
    // Recover the necessary part to be copied as a byte array.
    // This requires a copy, for now there is not a more efficient alternative.
    byte[] b = new byte[count];
    mByteBuffer.get(b, 0, count);
    other.getByteBuffer().put(b, 0, count);
  }
}
