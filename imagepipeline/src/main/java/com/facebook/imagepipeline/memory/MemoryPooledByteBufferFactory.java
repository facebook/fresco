/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteStreams;
import com.facebook.common.references.CloseableReference;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A factory to provide instances of {@link MemoryPooledByteBuffer} and {@link
 * MemoryPooledByteBufferOutputStream}
 */
@ThreadSafe
public class MemoryPooledByteBufferFactory implements PooledByteBufferFactory {

  private final PooledByteStreams mPooledByteStreams;
  private final MemoryChunkPool mPool; // memory pool

  public MemoryPooledByteBufferFactory(MemoryChunkPool pool, PooledByteStreams pooledByteStreams) {
    mPool = pool;
    mPooledByteStreams = pooledByteStreams;
  }

  @Override
  public MemoryPooledByteBuffer newByteBuffer(int size) {
    Preconditions.checkArgument(size > 0);
    CloseableReference<MemoryChunk> chunkRef = CloseableReference.of(mPool.get(size), mPool);
    try {
      return new MemoryPooledByteBuffer(chunkRef, size);
    } finally {
      chunkRef.close();
    }
  }

  @Override
  public MemoryPooledByteBuffer newByteBuffer(InputStream inputStream) throws IOException {
    MemoryPooledByteBufferOutputStream outputStream = new MemoryPooledByteBufferOutputStream(mPool);
    try {
      return newByteBuf(inputStream, outputStream);
    } finally {
      outputStream.close();
    }
  }

  @Override
  public MemoryPooledByteBuffer newByteBuffer(byte[] bytes) {
    MemoryPooledByteBufferOutputStream outputStream =
        new MemoryPooledByteBufferOutputStream(mPool, bytes.length);
    try {
      outputStream.write(bytes, 0, bytes.length);
      return outputStream.toByteBuffer();
    } catch (IOException ioe) {
      throw Throwables.propagate(ioe);
    } finally {
      outputStream.close();
    }
  }

  @Override
  public MemoryPooledByteBuffer newByteBuffer(InputStream inputStream, int initialCapacity)
      throws IOException {
    MemoryPooledByteBufferOutputStream outputStream =
        new MemoryPooledByteBufferOutputStream(mPool, initialCapacity);
    try {
      return newByteBuf(inputStream, outputStream);
    } finally {
      outputStream.close();
    }
  }

  /**
   * Reads all bytes from inputStream and writes them to outputStream. When all bytes are read
   * outputStream.toByteBuffer is called and obtained MemoryPooledByteBuffer is returned
   *
   * @param inputStream the input stream to read from
   * @param outputStream output stream used to transform content of input stream to
   *     MemoryPooledByteBuffer
   * @return an instance of MemoryPooledByteBuffer
   * @throws IOException
   */
  @VisibleForTesting
  MemoryPooledByteBuffer newByteBuf(
      InputStream inputStream, MemoryPooledByteBufferOutputStream outputStream) throws IOException {
    mPooledByteStreams.copy(inputStream, outputStream);
    return outputStream.toByteBuffer();
  }

  @Override
  public MemoryPooledByteBufferOutputStream newOutputStream() {
    return new MemoryPooledByteBufferOutputStream(mPool);
  }

  @Override
  public MemoryPooledByteBufferOutputStream newOutputStream(int initialCapacity) {
    return new MemoryPooledByteBufferOutputStream(mPool, initialCapacity);
  }
}
