/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.io.InputStream;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;


/**
 * An implementation of {@link PooledByteBuffer} that uses native memory
 * ({@link NativeMemoryChunk}) to store data
 */
@ThreadSafe
public class NativePooledByteBuffer implements PooledByteBuffer {

  private final int mSize;

  @GuardedBy("this")
  @VisibleForTesting
  CloseableReference<NativeMemoryChunk> mBufRef;

  public NativePooledByteBuffer(CloseableReference<NativeMemoryChunk> bufRef, int size) {
    Preconditions.checkNotNull(bufRef);
    Preconditions.checkArgument(size >= 0 && size <= bufRef.get().getSize());
    mBufRef = bufRef.clone();
    mSize = size;
  }

  /**
   * Gets the size of the bytebuffer if it is valid. Otherwise, an exception is raised
   * @return the size of the bytebuffer if it is not closed.
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
  public synchronized void read(int offset, byte[] buffer, int bufferOffset, int length) {
    ensureValid();
    // We need to make sure that PooledByteBuffer's length is preserved.
    // Al the other bounds checks will be performed by NativeMemoryChunk.read method.
    Preconditions.checkArgument(offset + length <= mSize);
    mBufRef.get().read(offset, buffer, bufferOffset, length);
  }

  @Override
  public synchronized long getNativePtr() {
    ensureValid();
    return mBufRef.get().getNativePtr();
  }

  /**
   * Check if this bytebuffer is already closed
   * @return true if this bytebuffer is closed.
   */
  @Override
  public synchronized boolean isClosed() {
    return !CloseableReference.isValid(mBufRef);
  }

  /**
   * Closes this instance, and releases the underlying buffer to the pool.
   * Once the bytebuffer has been closed, subsequent operations (especially {@code getStream()} will
   * fail.
   * Note: It is not an error to close an already closed bytebuffer
   */
  @Override
  public synchronized void close() {
    CloseableReference.closeSafely(mBufRef);
    mBufRef = null;
  }

  /**
   * Validates that the bytebuffer instance is valid (aka not closed). If it is closed, then we
   * raise a ClosedException
   * This doesn't really need to be synchronized, but lint won't shut up otherwise
   * @throws ClosedException
   */
  synchronized void ensureValid() {
    if (isClosed()) {
      throw new ClosedException();
    }
  }
}
