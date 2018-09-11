/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An implementation of {@link PooledByteBuffer} that uses ({@link MemoryChunk}) to
 * store data
 */
@ThreadSafe
public class MemoryPooledByteBuffer implements PooledByteBuffer {

  private final int mSize;

  @GuardedBy("this")
  @VisibleForTesting
  CloseableReference<MemoryChunk> mBufRef;

  public MemoryPooledByteBuffer(CloseableReference<MemoryChunk> bufRef, int size) {
    Preconditions.checkNotNull(bufRef);
    Preconditions.checkArgument(size >= 0 && size <= bufRef.get().getSize());
    mBufRef = bufRef.clone();
    mSize = size;
  }

  /**
   * Gets the size of the ByteBuffer if it is valid. Otherwise, an exception is raised
   *
   * @return the size of the ByteBuffer if it is not closed.
   * @throws {@link ClosedException}
   */
  @Override
  public synchronized int size() {
    ensureValid();
    return mSize;
  }

  @Override
  public synchronized byte read(int offset) {
    ensureValid();
    Preconditions.checkArgument(offset >= 0);
    Preconditions.checkArgument(offset < mSize);
    return mBufRef.get().read(offset);
  }

  @Override
  public synchronized int read(int offset, byte[] buffer, int bufferOffset, int length) {
    ensureValid();
    // We need to make sure that PooledByteBuffer's length is preserved.
    // Al the other bounds checks will be performed by NativeMemoryChunk.read method.
    Preconditions.checkArgument(offset + length <= mSize);
    return mBufRef.get().read(offset, buffer, bufferOffset, length);
  }

  @Override
  public synchronized long getNativePtr() throws UnsupportedOperationException {
    ensureValid();
    return mBufRef.get().getNativePtr();
  }

  @Override
  @Nullable
  public synchronized ByteBuffer getByteBuffer() {
    return mBufRef.get().getByteBuffer();
  }

  @Override
  public synchronized boolean isClosed() {
    return !CloseableReference.isValid(mBufRef);
  }

  /**
   * Closes this instance, and releases the underlying buffer to the pool. Once the ByteBuffer has
   * been closed, subsequent operations (especially {@code getStream()} will fail. Note: It is not
   * an error to close an already closed ByteBuffer
   */
  @Override
  public synchronized void close() {
    CloseableReference.closeSafely(mBufRef);
    mBufRef = null;
  }

  /**
   * Validates that the ByteBuffer instance is valid (aka not closed). If it is closed, then we
   * raise a ClosedException This doesn't really need to be synchronized, but lint won't shut up
   * otherwise
   *
   * @throws ClosedException
   */
  synchronized void ensureValid() {
    if (isClosed()) {
      throw new ClosedException();
    }
  }
}
