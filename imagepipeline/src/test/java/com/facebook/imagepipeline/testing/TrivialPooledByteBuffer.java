/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.imagepipeline.memory.PooledByteBuffer;

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
