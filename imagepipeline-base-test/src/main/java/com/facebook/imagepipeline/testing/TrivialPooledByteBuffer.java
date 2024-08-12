/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.infer.annotation.Nullsafe;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/** A trivial implementation of {@link PooledByteBuffer} */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class TrivialPooledByteBuffer implements PooledByteBuffer {
  @Nullable private byte[] mBuf;
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
    // NULLSAFE_FIXME[Nullable Dereference]
    return isClosed() ? -1 : mBuf.length;
  }

  @Override
  public byte read(int offset) {
    // NULLSAFE_FIXME[Nullable Dereference]
    return mBuf[offset];
  }

  @Override
  public int read(int offset, byte[] buffer, int bufferOffset, int length) {
    // NULLSAFE_FIXME[Parameter Not Nullable]
    System.arraycopy(mBuf, offset, buffer, bufferOffset, length);
    return length;
  }

  @Override
  public long getNativePtr() {
    return mNativePtr;
  }

  @Override
  @Nullable
  public ByteBuffer getByteBuffer() {
    return null;
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
