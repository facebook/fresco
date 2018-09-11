/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.memory;

import android.util.Log;
import com.facebook.common.internal.Preconditions;
import java.io.Closeable;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/**
 * Wrapper around chunk using a direct ByteBuffer in native memory. A direct ByteBuffer is composed
 * of a Java {@link ByteBuffer} object allocated on the Java heap and the underlying buffer which is
 * in native memory.
 *
 * <p>The buffer in native memory will be released when the Java object gets garbage collected.
 */
public class BufferMemoryChunk implements MemoryChunk, Closeable {
  private static final String TAG = "BufferMemoryChunk";

  /** Internal representation of the chunk */
  private ByteBuffer mBuffer;

  /** Size of the ByteBuffer */
  private final int mSize;

  /** Unique identifier of the chunk */
  private final long mId;

  public BufferMemoryChunk(final int size) {
    mBuffer = ByteBuffer.allocateDirect(size);
    mSize = size;
    mId = System.identityHashCode(this);
  }

  @Override
  public synchronized void close() {
    mBuffer = null;
  }

  @Override
  public synchronized boolean isClosed() {
    return mBuffer == null;
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
    mBuffer.position(memoryOffset);
    mBuffer.put(byteArray, byteArrayOffset, actualCount);
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
    mBuffer.position(memoryOffset);
    mBuffer.get(byteArray, byteArrayOffset, actualCount);
    return actualCount;
  }

  @Override
  public synchronized byte read(final int offset) {
    Preconditions.checkState(!isClosed());
    Preconditions.checkArgument(offset >= 0);
    Preconditions.checkArgument(offset < mSize);
    return mBuffer.get(offset);
  }

  @Override
  public void copy(
      final int offset, final MemoryChunk other, final int otherOffset, final int count) {
    Preconditions.checkNotNull(other);
    // This implementation acquires locks on this and other objects and then delegates to
    // doCopy which does actual copy. In order to avoid deadlocks we have to establish some linear
    // order on all BufferMemoryChunks and acquire locks according to this order. In order
    // to do that, we use unique ids.
    // So we have to address 3 cases:

    // Case 1: other buffer equals this buffer, id comparison
    if (other.getUniqueId() == getUniqueId()) {
      // we do not allow copying to the same address
      // lets log warning and not copy
      Log.w(
          TAG,
          "Copying from BufferMemoryChunk "
              + Long.toHexString(getUniqueId())
              + " to BufferMemoryChunk "
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

  @Override
  public long getNativePtr() {
    throw new UnsupportedOperationException("Cannot get the pointer of a BufferMemoryChunk");
  }

  @Override
  @Nullable
  public synchronized ByteBuffer getByteBuffer() {
    return mBuffer;
  }

  @Override
  public long getUniqueId() {
    return mId;
  }

  /**
   * This does actual copy. It should be called only when we hold locks on both this and other
   * objects
   */
  private void doCopy(
      final int offset, final MemoryChunk other, final int otherOffset, final int count) {
    if (!(other instanceof BufferMemoryChunk)) {
      throw new IllegalArgumentException("Cannot copy two incompatible MemoryChunks");
    }
    Preconditions.checkState(!isClosed());
    Preconditions.checkState(!other.isClosed());
    MemoryChunkUtil.checkBounds(offset, other.getSize(), otherOffset, count, mSize);
    mBuffer.position(offset);
    // ByteBuffer can't be null at this point
    other.getByteBuffer().position(otherOffset);
    // Recover the necessary part to be copied as a byte array.
    // This requires a copy, for now there is not a more efficient alternative.
    byte[] b = new byte[count];
    mBuffer.get(b, 0, count);
    other.getByteBuffer().put(b, 0, count);
  }
}
