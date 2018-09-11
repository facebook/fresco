/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.testing;

import com.facebook.common.memory.PooledByteBuffer;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/** A trivial implementation of {@link PooledByteBuffer} using {@link ByteBuffer} */
public class TrivialBufferPooledByteBuffer implements PooledByteBuffer {
  private ByteBuffer mBuffer;

  public TrivialBufferPooledByteBuffer(byte[] buf) {
    mBuffer = ByteBuffer.allocateDirect(buf.length);
    mBuffer.put(buf);
  }

  @Override
  public int size() {
    return isClosed() ? -1 : mBuffer.capacity();
  }

  @Override
  public byte read(int offset) {
    return mBuffer.get(offset);
  }

  @Override
  public int read(int offset, byte[] buffer, int bufferOffset, int length) {
    mBuffer.position(offset);
    mBuffer.get(buffer, bufferOffset, length);
    return length;
  }

  @Override
  public long getNativePtr() {
    return 0L;
  }

  @Override
  @Nullable
  public ByteBuffer getByteBuffer() {
    return mBuffer;
  }

  @Override
  public boolean isClosed() {
    return mBuffer == null;
  }

  @Override
  public void close() {
    mBuffer = null;
  }
}
