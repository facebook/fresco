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
 * An InputStream implementation over a {@link NativeMemoryChunk} instance
 */
@NotThreadSafe
public class NativeMemoryChunkInputStream extends InputStream {

  @VisibleForTesting
  final NativeMemoryChunk mMemoryChunk; // the underlying memory chunk we're using

  @VisibleForTesting
  final int mEndOffset; // end offset in chunk

  @VisibleForTesting
  final int mStartOffset; // initial offset into the chunk

  @VisibleForTesting
  int mOffset; // current offset in the chunk
  @VisibleForTesting
  int mMark; // position of 'mark' if any

  // to avoid new allocation on every read()
  byte[] mSingleByteBuffer = new byte[1];

  /**
   * Creates a new inputstream instance over the specific memory chunk.
   * @param nativeMemoryChunk the native memory chunk
   * @param startOffset start offset within the memory chunk
   * @param length length of subchunk
   */
  public NativeMemoryChunkInputStream(
      NativeMemoryChunk nativeMemoryChunk,
      int startOffset,
      int length) {
    super();
    Preconditions.checkState(startOffset >= 0);
    Preconditions.checkState(length >= 0);
    mMemoryChunk = Preconditions.checkNotNull(nativeMemoryChunk);
    mStartOffset = startOffset;
    mEndOffset = startOffset + length > nativeMemoryChunk.getSize()
        ? nativeMemoryChunk.getSize() : startOffset + length;
    mOffset = startOffset;
    mMark = startOffset;
  }

  /**
   * Returns the number of bytes still available to read
   */
  @Override
  public int available() {
    return Math.max(0, mEndOffset - mOffset);
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
  public int read() throws IOException {
    int len = this.read(mSingleByteBuffer, 0, 1);
    int b = (int) mSingleByteBuffer[0] & 0xFF;
    return (len == 1) ? b : -1;
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

    if (mOffset >= mEndOffset) {
      return -1;
    }

    if (length == 0) {
      return 0;
    }

    int numToRead = Math.min(available(), length);
    int numRead = mMemoryChunk.read(mOffset, buffer, offset, numToRead);
    mOffset += numRead;
    return numRead;
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
   * @throws IOException
   */
  @Override
  public long skip(long byteCount) {
    // skip-count is zero or less; or we're already past the end of stream? Just return 0
    if (byteCount <= 0 ||
        (mOffset >= mEndOffset)) {
      return 0;
    }

    int temp = mOffset;
    mOffset = (mEndOffset - mOffset) < byteCount ? mEndOffset : (int)(mOffset + byteCount);
    return Math.max(0, mOffset - temp);
  }
}
