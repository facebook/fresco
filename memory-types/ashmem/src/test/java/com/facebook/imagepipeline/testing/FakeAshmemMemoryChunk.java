/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.imagepipeline.memory.AshmemMemoryChunk;
import com.facebook.imagepipeline.memory.MemoryChunk;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * A fake implementation of {@link com.facebook.memory.ashmem.AshmemMemoryChunk} to allow us to test
 * out pools and other functionality. This uses byte arrays instead of actual ashmem memory, but
 * supports the same public interface
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class FakeAshmemMemoryChunk extends AshmemMemoryChunk {
  @Nullable private byte[] mBuf;

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
    // NULLSAFE_FIXME[Nullable Dereference]
    return mBuf.length;
  }

  @Override
  public int write(int ashmemMemoryOffset, byte[] byteArray, int byteArrayOffset, int count) {
    // NULLSAFE_FIXME[Nullable Dereference]
    int numToWrite = Math.min(count, mBuf.length - ashmemMemoryOffset);
    // NULLSAFE_FIXME[Parameter Not Nullable]
    System.arraycopy(byteArray, byteArrayOffset, mBuf, ashmemMemoryOffset, numToWrite);
    return numToWrite;
  }

  @Override
  public byte read(int ashmemMemoryOffset) {
    // NULLSAFE_FIXME[Nullable Dereference]
    return mBuf[ashmemMemoryOffset];
  }

  @Override
  public int read(int ashmemMemoryOffset, byte[] byteArray, int byteArrayOffset, int count) {
    // NULLSAFE_FIXME[Nullable Dereference]
    int numToRead = Math.min(count, mBuf.length - ashmemMemoryOffset);
    // NULLSAFE_FIXME[Parameter Not Nullable]
    System.arraycopy(mBuf, ashmemMemoryOffset, byteArray, byteArrayOffset, numToRead);
    return numToRead;
  }

  @Override
  public void copy(int offset, MemoryChunk other, int otherOffset, int count) {
    FakeAshmemMemoryChunk that = (FakeAshmemMemoryChunk) other;
    // NULLSAFE_FIXME[Nullable Dereference]
    int numToCopy = Math.min(count, mBuf.length - offset);
    // NULLSAFE_FIXME[Parameter Not Nullable]
    System.arraycopy(mBuf, offset, that.mBuf, otherOffset, numToCopy);
  }
}
