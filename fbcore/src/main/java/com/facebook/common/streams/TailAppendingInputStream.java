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
import java.lang.Override;

/**
 * InputStream that returns all bytes from another stream, then appends the specified 'tail' bytes.
 */
public class TailAppendingInputStream extends FilterInputStream {
  private final byte[] mTail;
  private int mTailOffset;
  private int mMarkedTailOffset;

  public TailAppendingInputStream(InputStream inputStream, byte[] tail) {
    super(inputStream);
    if (inputStream == null) {
      throw new NullPointerException();
    }
    if (tail == null) {
      throw new NullPointerException();
    }
    mTail = tail;
  }

  @Override
  public int read() throws IOException {
    final int readResult = in.read();
    if (readResult != -1) {
      return readResult;
    }
    return readNextTailByte();
  }

  @Override
  public int read(byte[] buffer) throws IOException {
    return read(buffer, 0, buffer.length);
  }

  @Override
  public int read(byte[] buffer, int offset, int count) throws IOException {
    final int readResult = in.read(buffer, offset, count);
    if (readResult != -1) {
      return readResult;
    }

    if (count == 0) {
      return 0;
    }

    int bytesRead = 0;
    while (bytesRead < count) {
      final int nextByte = readNextTailByte();
      if (nextByte == -1) {
        break;
      }
      buffer[offset + bytesRead] = (byte) nextByte;
      bytesRead++;
    }
    return bytesRead > 0 ? bytesRead : -1;
  }

  @Override
  public void reset() throws IOException {
    if (in.markSupported()) {
      in.reset();
      mTailOffset = mMarkedTailOffset;
    } else {
      throw new IOException("mark is not supported");
    }
  }

  @Override
  public void mark(int readLimit) {
    if (in.markSupported()) {
      super.mark(readLimit);
      mMarkedTailOffset = mTailOffset;
    }
  }

  private int readNextTailByte() {
    if (mTailOffset >= mTail.length) {
      return -1;
    }
    return ((int) mTail[mTailOffset++]) & 0xFF;
  }
}
