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
import com.facebook.common.logging.FLog;
import com.facebook.common.references.ResourceReleaser;

/**
 * InputStream that wraps another input stream and buffers all reads.
 *
 * <p> For purpose of buffering a byte array is used. It is provided during construction time
 * together with ResourceReleaser responsible for releasing it when the stream is closed.
 */
@NotThreadSafe
public class PooledByteArrayBufferedInputStream extends InputStream {

  private static final String TAG = "PooledByteInputStream";

  private final InputStream mInputStream;
  private final byte[] mByteArray;
  private final ResourceReleaser<byte[]> mResourceReleaser;

  /**
   * how many bytes in mByteArray were set by last call to mInputStream.read
   */
  private int mBufferedSize;
  /**
   * position of next buffered byte in mByteArray to be read
   *
   * <p> invariant: 0 <= mBufferOffset <= mBufferedSize
   */
  private int mBufferOffset;

  private boolean mClosed;

  public PooledByteArrayBufferedInputStream(
      InputStream inputStream,
      byte[] byteArray,
      ResourceReleaser<byte[]> resourceReleaser) {
    mInputStream = Preconditions.checkNotNull(inputStream);
    mByteArray = Preconditions.checkNotNull(byteArray);
    mResourceReleaser = Preconditions.checkNotNull(resourceReleaser);
    mBufferedSize = 0;
    mBufferOffset = 0;
    mClosed = false;
  }

  @Override
  public int read() throws IOException {
    Preconditions.checkState(mBufferOffset <= mBufferedSize);
    ensureNotClosed();
    if (!ensureDataInBuffer()) {
      return -1;
    }

    return mByteArray[mBufferOffset++] & 0xFF;
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    Preconditions.checkState(mBufferOffset <= mBufferedSize);
    ensureNotClosed();
    if (!ensureDataInBuffer()) {
      return -1;
    }

    final int bytesToRead = Math.min(mBufferedSize - mBufferOffset, length);
    System.arraycopy(mByteArray, mBufferOffset, buffer, offset, bytesToRead);
    mBufferOffset += bytesToRead;
    return bytesToRead;
  }

  @Override
  public int available() throws IOException {
    Preconditions.checkState(mBufferOffset <= mBufferedSize);
    ensureNotClosed();
    return mBufferedSize - mBufferOffset + mInputStream.available();
  }

  @Override
  public void close() throws IOException {
    if (!mClosed) {
      mClosed = true;
      mResourceReleaser.release(mByteArray);
      super.close();
    }
  }

  @Override
  public long skip(long byteCount) throws IOException {
    Preconditions.checkState(mBufferOffset <= mBufferedSize);
    ensureNotClosed();
    final int bytesLeftInBuffer = mBufferedSize - mBufferOffset;
    if (bytesLeftInBuffer >= byteCount) {
      mBufferOffset += byteCount;
      return byteCount;
    }

    mBufferOffset = mBufferedSize;
    return bytesLeftInBuffer + mInputStream.skip(byteCount - bytesLeftInBuffer);
  }

  /**
   * Checks if there is some data left in the buffer. If not but buffered stream still has some
   * data to be read, then more data is buffered.
   *
   * @return false if and only if there is no more data and underlying input stream has no more data
   *   to be read
   * @throws IOException
   */
  private boolean ensureDataInBuffer() throws IOException {
    if (mBufferOffset < mBufferedSize) {
      return true;
    }

    final int readData = mInputStream.read(mByteArray);
    if (readData <= 0) {
      return false;
    }

    mBufferedSize = readData;
    mBufferOffset = 0;
    return true;
  }

  private void ensureNotClosed() throws IOException {
    if (mClosed) {
      throw new IOException("stream already closed");
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (!mClosed) {
      FLog.e(TAG, "Finalized without closing");
      close();
    }
    super.finalize();
  }
}
