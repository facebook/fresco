/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.IOException;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;


/**
 * An implementation of {@link PooledByteBufferOutputStream} that produces a
 * {@link NativePooledByteBuffer}
 */
@NotThreadSafe
public class NativePooledByteBufferOutputStream extends PooledByteBufferOutputStream {
  private final NativeMemoryChunkPool mPool;  // the pool to allocate memory chunks from
  private CloseableReference<NativeMemoryChunk> mBufRef; // the current chunk that we're writing to
  private int mCount; // number of bytes 'used' in the current chunk

  /**
   * Construct a new instance of this outputstream
   * @param pool the pool to use
   */
  public NativePooledByteBufferOutputStream(NativeMemoryChunkPool pool) {
    this(pool, pool.getMinBufferSize());
  }

  /**
   * Construct a new instance of this output stream with this initial capacity
   * It is not an error to have this initial capacity be inaccurate. If the actual contents
   * end up being larger than the initialCapacity, then we will reallocate memory
   * if needed. If the actual contents are smaller, then we'll end up wasting some memory
   * @param pool the pool to use
   * @param initialCapacity initial capacity to allocate for this stream
   */
  public NativePooledByteBufferOutputStream(NativeMemoryChunkPool pool, int initialCapacity) {
    super();

    Preconditions.checkArgument(initialCapacity > 0);
    mPool = Preconditions.checkNotNull(pool);
    mCount = 0;
    mBufRef = CloseableReference.of(mPool.get(initialCapacity), mPool);
  }

  /**
   * Gets a PooledByteBuffer from the current contents. If the stream has already been closed, then
   * an InvalidStreamException is thrown.
   * @return a PooledByteBuffer instance for the contents of the stream
   * @throws InvalidStreamException if the stream is invalid
   */
  @Override
  public NativePooledByteBuffer toByteBuffer() {
    ensureValid();
    return new NativePooledByteBuffer(mBufRef, mCount);
  }

  /**
   * Returns the total number of bytes written to this stream so far.
   * @return the number of bytes written to this stream.
   */
  @Override
  public int size() {
    return mCount;
  }

  /**
   * Write one byte to the underlying stream. The underlying stream MUST be valid
   * @param oneByte the one byte to write
   * @throws InvalidStreamException if the stream is invalid
   * @throws IOException in case of an I/O error during the write
   */
  @Override
  public void write(int oneByte) throws IOException {
    byte[] buf = new byte[1];
    buf[0] = (byte)oneByte;
    this.write(buf);
  }

  /**
   * Writes {@code count} bytes from the byte array {@code buffer} starting at
   * position {@code offset} to this stream.
   * The underlying stream MUST be valid
   *
   * @param buffer the source buffer to read from
   * @param offset the start position in {@code buffer} from where to get bytes.
   * @param count the number of bytes from {@code buffer} to write to this stream.
   * @throws IOException if an error occurs while writing to this stream.
   * @throws IndexOutOfBoundsException
   *             if {@code offset < 0} or {@code count < 0}, or if
   *             {@code offset + count} is bigger than the length of
   *             {@code buffer}.
   * @throws InvalidStreamException if the stream is invalid
   */
  public void write(byte[] buffer, int offset, int count) throws IOException {
    if (offset < 0 || count < 0 || offset + count > buffer.length) {
      throw new ArrayIndexOutOfBoundsException("length=" + buffer.length + "; regionStart=" + offset
          + "; regionLength=" + count);
    }
    ensureValid();
    realloc(mCount + count);
    mBufRef.get().write(mCount, buffer, offset, count);
    mCount += count;
  }

  /**
   * Closes the stream. Owned resources are released back to the pool. It is not allowed to call
   * toByteBuffer after call to this method.
   * @throws IOException
   */
  @Override
  public void close() {
    CloseableReference.closeSafely(mBufRef);
    mBufRef = null;
    mCount = -1;
    super.close();
  }

  /**
   * Reallocate the local buffer to hold the new length specified.
   * Also copy over existing data to this new buffer
   * @param newLength new length of buffer
   * @throws InvalidStreamException if the stream is invalid
   * @throws BasePool.SizeTooLargeException if the allocation from the pool fails
   */
  @VisibleForTesting
  void realloc(int newLength) {
    ensureValid();
    /* Can the buffer handle @i more bytes, if not expand it */
    if (newLength <= mBufRef.get().getSize()) {
      return;
    }
    NativeMemoryChunk newbuf = mPool.get(newLength);
    mBufRef.get().copy(0, newbuf, 0, mCount);
    mBufRef.close();
    mBufRef = CloseableReference.of(newbuf, mPool);
  }

  /**
   * Ensure that the current stream is valid, that is underlying closeable reference is not null
   * and is valid
   * @throws InvalidStreamException if the stream is invalid
   */
  private void ensureValid() {
    if (!CloseableReference.isValid(mBufRef)) {
      throw new InvalidStreamException();
    }
  }

  /**
   * An exception indicating that this stream is no longer valid
   */
  public static class InvalidStreamException extends RuntimeException {
    public InvalidStreamException() {
      super("OutputStream no longer valid");
    }
  }
}
