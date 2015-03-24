/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import javax.annotation.concurrent.ThreadSafe;

import java.io.IOException;
import java.io.InputStream;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;

/**
 * A factory to provide instances of {@link NativePooledByteBuffer} and
 * {@link NativePooledByteBufferOutputStream}
 */
@ThreadSafe
public class NativePooledByteBufferFactory implements PooledByteBufferFactory {

  private final PooledByteStreams mPooledByteStreams;
  private final NativeMemoryChunkPool mPool;    // native memory pool

  public NativePooledByteBufferFactory(
      NativeMemoryChunkPool pool,
      PooledByteStreams pooledByteStreams) {
    mPool = pool;
    mPooledByteStreams = pooledByteStreams;
  }

  @Override
  public NativePooledByteBuffer newByteBuffer(int size) {
    Preconditions.checkArgument(size > 0);
    CloseableReference<NativeMemoryChunk> chunkRef = CloseableReference.of(mPool.get(size), mPool);
    try {
      return new NativePooledByteBuffer(chunkRef, size);
    } finally {
      chunkRef.close();
    }
  }

  /**
   * Creates a new NativePooledByteBuffer instance by reading in the entire contents of the
   * input stream
   * @param inputStream the input stream to read from
   * @return an instance of the NativePooledByteBuffer
   * @throws IOException
   */
  @Override
  public NativePooledByteBuffer newByteBuffer(InputStream inputStream) throws IOException {
    NativePooledByteBufferOutputStream outputStream = new NativePooledByteBufferOutputStream(mPool);
    try {
      return newByteBuf(inputStream, outputStream);
    } finally {
      outputStream.close();
    }
  }

  /**
   * Creates a new NativePooledByteBuffer instance by reading in the entire contents of the
   * byte array
   * @param bytes the byte array to read from
   * @return an instance of the NativePooledByteBuffer
   */
  @Override
  public NativePooledByteBuffer newByteBuffer(byte[] bytes) {
    NativePooledByteBufferOutputStream outputStream =
        new NativePooledByteBufferOutputStream(mPool, bytes.length);
    try {
      outputStream.write(bytes, 0, bytes.length);
      return outputStream.toByteBuffer();
    } catch (IOException ioe) {
      throw Throwables.propagate(ioe);
    } finally {
      outputStream.close();
    }
  }

  /**
   * Creates a new NativePooledByteBuffer instance with an initial capacity, and reading the entire
   * contents of the input stream
   * @param inputStream the input stream to read from
   * @param initialCapacity initial allocation size for the PooledByteBuffer
   * @return an instance of NativePooledByteBuffer
   * @throws IOException
   */
  @Override
  public NativePooledByteBuffer newByteBuffer(InputStream inputStream, int initialCapacity)
      throws IOException {
    NativePooledByteBufferOutputStream outputStream =
        new NativePooledByteBufferOutputStream(mPool, initialCapacity);
    try {
      return newByteBuf(inputStream, outputStream);
    } finally {
      outputStream.close();
    }
  }

  /**
   * Reads all bytes from inputStream and writes them to outputStream. When all bytes
   * are read outputStream.toByteBuffer is called and obtained NativePooledByteBuffer is returned
   * @param inputStream the input stream to read from
   * @param outputStream output stream used to transform content of input stream to
   *   NativePooledByteBuffer
   * @return an instance of NativePooledByteBuffer
   * @throws IOException
   */
  @VisibleForTesting
  NativePooledByteBuffer newByteBuf(
      InputStream inputStream,
      NativePooledByteBufferOutputStream outputStream)
      throws IOException {
    mPooledByteStreams.copy(inputStream, outputStream);
    return outputStream.toByteBuffer();
  }

  /**
   * Creates a new NativePooledByteBufferOutputStream instance with default initial capacity
   * @return a new NativePooledByteBufferOutputStream
   */
  @Override
  public NativePooledByteBufferOutputStream newOutputStream() {
    return new NativePooledByteBufferOutputStream(mPool);
  }

  /**
   * Creates a new NativePooledByteBufferOutputStream instance with the specified initial capacity
   * @param initialCapacity initial allocation size for the underlying output stream
   * @return a new NativePooledByteBufferOutputStream
   */
  @Override
  public NativePooledByteBufferOutputStream newOutputStream(int initialCapacity) {
    return new NativePooledByteBufferOutputStream(mPool, initialCapacity);
  }
}
