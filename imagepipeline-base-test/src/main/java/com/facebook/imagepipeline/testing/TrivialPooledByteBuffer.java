/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.common.memory.PooledByteBuffer;

/**
 * A trivial implementation of {@link PooledByteBuffer}
 */
public class TrivialPooledByteBuffer implements PooledByteBuffer {
  private byte[] mBuf;
  private long mNativePtr;

  public TrivialPooledByteBuffer(byte[] buf) {
    this(buf, 0L);
  }

  public TrivialPooledByteBuffer(byte[] buf, long nativePtr) {
    mBuf = buf;
    mNativePtr = nativePtr;
  }

  @Override
  public int size() {
    return isClosed() ? -1 : mBuf.length;
  }

  @Override
  public byte read(int offset) {
    return mBuf[offset];
  }

  @Override
  public void read(int offset, byte[] buffer, int bufferOffset, int length) {
    System.arraycopy(mBuf, offset, buffer, bufferOffset, length);
  }

  @Override
  public long getNativePtr() {
    return mNativePtr;
  }

  @Override
  public boolean isClosed() {
    return mBuf == null;
  }

  @Override
  public void close() {
    mBuf = null;
  }
}
