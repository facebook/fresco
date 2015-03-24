/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.streams;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the wrapped InputStream only until a specified number of bytes, the 'limit' is reached.
 */
public class LimitedInputStream extends FilterInputStream {
  private int mBytesToRead;
  private int mBytesToReadWhenMarked;

  public LimitedInputStream(InputStream inputStream, int limit) {
    super(inputStream);
    if (inputStream == null) {
      throw new NullPointerException();
    }
    if (limit < 0) {
      throw new IllegalArgumentException("limit must be >= 0");
    }
    mBytesToRead = limit;
    mBytesToReadWhenMarked = -1;
  }

  @Override
  public int read() throws IOException {
    if (mBytesToRead == 0) {
      return -1;
    }

    final int readByte = in.read();
    if (readByte != -1) {
      mBytesToRead--;
    }

    return readByte;
  }

  @Override
  public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
    if (mBytesToRead == 0) {
      return -1;
    }

    final int maxBytesToRead = Math.min(byteCount, mBytesToRead);
    final int bytesRead = in.read(buffer, byteOffset, maxBytesToRead);
    if (bytesRead > 0) {
      mBytesToRead -= bytesRead;
    }

    return bytesRead;
  }

  @Override
  public long skip(long byteCount) throws IOException {
    final long maxBytesToSkip = Math.min(byteCount, mBytesToRead);
    final long bytesSkipped = in.skip(maxBytesToSkip);
    mBytesToRead -= bytesSkipped;
    return bytesSkipped;
  }

  @Override
  public int available() throws IOException {
    return Math.min(in.available(), mBytesToRead);
  }

  @Override
  public void mark(int readLimit) {
    if (in.markSupported()) {
      in.mark(readLimit);
      mBytesToReadWhenMarked = mBytesToRead;
    }
  }

  @Override
  public void reset() throws IOException {
    if (!in.markSupported()) {
      throw new IOException("mark is not supported");
    }

    if (mBytesToReadWhenMarked == -1) {
      throw new IOException("mark not set");
    }

    in.reset();
    mBytesToRead = mBytesToReadWhenMarked;
  }
}
