/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.IOException;
import java.io.InputStream;

/**
 * A factory to create instances of PooledByteBuffer and PooledByteBufferOutputStream
 */
public interface PooledByteBufferFactory {

  /**
   * Creates a new PooledByteBuffer instance of given size.
   * @param size in bytes
   * @return an instance of PooledByteBuffer
   */
  PooledByteBuffer newByteBuffer(int size);

  /**
   * Creates a new bytebuf instance by reading in the entire contents of the input stream
   * @param inputStream the input stream to read from
   * @return an instance of the PooledByteBuffer
   * @throws IOException
   */
  PooledByteBuffer newByteBuffer(InputStream inputStream) throws IOException;

  /**
   * Creates a new bytebuf instance by reading in the entire contents of the byte array
   * @param bytes the byte array to read from
   * @return an instance of the PooledByteBuffer
   */
  PooledByteBuffer newByteBuffer(byte[] bytes);

  /**
   * Creates a new PooledByteBuffer instance with an initial capacity, and reading the entire
   * contents of the input stream
   * @param inputStream the input stream to read from
   * @param initialCapacity initial allocation size for the bytebuf
   * @return an instance of PooledByteBuffer
   * @throws IOException
   */
  PooledByteBuffer newByteBuffer(InputStream inputStream, int initialCapacity) throws IOException;

  /**
   * Creates a new PooledByteBufferOutputStream instance with default initial capacity
   * @return a new PooledByteBufferOutputStream
   */
  PooledByteBufferOutputStream newOutputStream();

  /**
   * Creates a new PooledByteBufferOutputStream instance with the specified initial capacity
   * @param initialCapacity initial allocation size for the underlying output stream
   * @return a new PooledByteBufferOutputStream
   */
  PooledByteBufferOutputStream newOutputStream(int initialCapacity);
}
