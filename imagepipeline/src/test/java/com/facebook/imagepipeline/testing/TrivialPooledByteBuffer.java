// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.imagepipeline.testing;

import java.io.InputStream;

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
