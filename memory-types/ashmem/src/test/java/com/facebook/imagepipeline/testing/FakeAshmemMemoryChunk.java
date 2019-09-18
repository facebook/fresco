/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.imagepipeline.memory.AshmemMemoryChunk;
import com.facebook.imagepipeline.memory.MemoryChunk;

/**
 * A fake implementation of {@link com.facebook.memory.ashmem.AshmemMemoryChunk} to allow us to test
 * out pools and other functionality. This uses byte arrays instead of actual ashmem memory, but
 * supports the same public interface
 */
public class FakeAshmemMemoryChunk extends AshmemMemoryChunk {
  private byte[] mBuf;

  public FakeAshmemMemoryChunk(int bufSize) {
    super();
    mBuf = new byte[bufSize];
  }

  @Override
  public void close() {
    mBuf = null;
  }

  @Override
  public boolean isClosed() {
    return mBuf == null;
  }

  @Override
  public int getSize() {
    return mBuf.length;
  }

  @Override
  public int write(int ashmemMemoryOffset, byte[] byteArray, int byteArrayOffset, int count) {
    int numToWrite = Math.min(count, mBuf.length - ashmemMemoryOffset);
    System.arraycopy(byteArray, byteArrayOffset, mBuf, ashmemMemoryOffset, numToWrite);
    return numToWrite;
  }

  @Override
  public byte read(int ashmemMemoryOffset) {
    return mBuf[ashmemMemoryOffset];
  }

  @Override
  public int read(int ashmemMemoryOffset, byte[] byteArray, int byteArrayOffset, int count) {
    int numToRead = Math.min(count, mBuf.length - ashmemMemoryOffset);
    System.arraycopy(mBuf, ashmemMemoryOffset, byteArray, byteArrayOffset, numToRead);
    return numToRead;
  }

  @Override
  public void copy(int offset, MemoryChunk other, int otherOffset, int count) {
    FakeAshmemMemoryChunk that = (FakeAshmemMemoryChunk) other;
    int numToCopy = Math.min(count, mBuf.length - offset);
    System.arraycopy(mBuf, offset, that.mBuf, otherOffset, numToCopy);
  }
}
