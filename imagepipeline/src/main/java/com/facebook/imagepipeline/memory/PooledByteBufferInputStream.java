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
import java.io.InputStream;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * An InputStream implementation over a {@link PooledByteBuffer} instance
 */
@NotThreadSafe
public class PooledByteBufferInputStream extends InputStream {

  @VisibleForTesting
  final PooledByteBuffer mPooledByteBuffer;

  @VisibleForTesting
  int mOffset; // current offset in the chunk
  @VisibleForTesting
  int mMark; // position of 'mark' if any

  /**
   * Creates a new inputstream instance over the specific buffer.
   * @param pooledByteBuffer the buffer to read from
   */
  public PooledByteBufferInputStream(PooledByteBuffer pooledByteBuffer) {
    super();
    Preconditions.checkArgument(!pooledByteBuffer.isClosed());
    mPooledByteBuffer = Preconditions.checkNotNull(pooledByteBuffer);
    mOffset = 0;
    mMark = 0;
  }

  /**
   * Returns the number of bytes still available to read
   */
  @Override
  public int available() {
    return mPooledByteBuffer.size() - mOffset;
  }

  /**
   * Sets a mark position in this inputstream.
   * The parameter {@code readlimit} is ignored.
   * Sending {@link #reset()}  will reposition the stream back to the marked position.
   * @param readlimit ignored.
   */
  @Override
  public void mark(int readlimit) {
    mMark = mOffset;
  }

  /**
   * Returns {@code true} since this class supports {@link #mark(int)} and {@link #reset()}
   * methods
   */
  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public int read() {
    if (available() <= 0) {
      return -1;
    }
    return ((int) mPooledByteBuffer.read(mOffset++))  & 0xFF;
  }

  @Override
  public int read(byte[] buffer) {
    return read(buffer, 0, buffer.length);
  }

  /**
   * Reads at most {@code length} bytes from this stream and stores them in byte array
   * {@code buffer} starting at {@code offset}.
   * @param buffer the buffer to read data into
   * @param offset start offset in the buffer
   * @param length max number of bytes to read
   * @return number of bytes read
   */
  @Override
  public int read(byte[] buffer, int offset, int length) {
    if (offset < 0 || length < 0 || offset + length > buffer.length) {
      throw new ArrayIndexOutOfBoundsException(
          "length=" + buffer.length +
          "; regionStart=" + offset +
          "; regionLength=" + length);
    }

    final int available = available();
    if (available <= 0) {
      return -1;
    }

    if (length <= 0) {
      return 0;
    }

    int numToRead = Math.min(available, length);
    mPooledByteBuffer.read(mOffset, buffer, offset, numToRead);
    mOffset += numToRead;
    return numToRead;
  }

  /**
   * Resets this stream to the last marked location. This implementation
   * resets the position to either the marked position, the start position
   * supplied in the constructor or 0 if neither has been provided.
   */
  @Override
  public void reset() {
    mOffset = mMark;
  }

  /**
   * Skips byteCount (or however many bytes are available) bytes in the stream
   * @param byteCount number of bytes to skip
   * @return number of bytes actually skipped
   */
  @Override
  public long skip(long byteCount) {
    Preconditions.checkArgument(byteCount >= 0);
    int skipped = Math.min((int) byteCount, available());
    mOffset += skipped;
    return skipped;
  }
}
