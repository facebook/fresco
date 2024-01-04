/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.Throwables
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.common.memory.PooledByteStreams
import com.facebook.common.references.CloseableReference
import java.io.IOException
import java.io.InputStream
import javax.annotation.concurrent.ThreadSafe

/**
 * A factory to provide instances of [MemoryPooledByteBuffer] and
 * [MemoryPooledByteBufferOutputStream]
 */
@ThreadSafe
class MemoryPooledByteBufferFactory( // memory pool
    private val pool: MemoryChunkPool,
    private val pooledByteStreams: PooledByteStreams
) : PooledByteBufferFactory {

  override fun newByteBuffer(size: Int): MemoryPooledByteBuffer {
    check(size > 0)
    val chunkRef = CloseableReference.of(pool[size], pool)
    return try {
      MemoryPooledByteBuffer(chunkRef, size)
    } finally {
      chunkRef.close()
    }
  }

  @Throws(IOException::class)
  override fun newByteBuffer(inputStream: InputStream): MemoryPooledByteBuffer {
    val outputStream = MemoryPooledByteBufferOutputStream(pool)
    return try {
      newByteBuf(inputStream, outputStream)
    } finally {
      outputStream.close()
    }
  }

  override fun newByteBuffer(bytes: ByteArray): MemoryPooledByteBuffer {
    val outputStream = MemoryPooledByteBufferOutputStream(pool, bytes.size)
    return try {
      outputStream.write(bytes, 0, bytes.size)
      outputStream.toByteBuffer()
    } catch (ioe: IOException) {
      throw Throwables.propagate(ioe)
    } finally {
      outputStream.close()
    }
  }

  @Throws(IOException::class)
  override fun newByteBuffer(
      inputStream: InputStream,
      initialCapacity: Int
  ): MemoryPooledByteBuffer {
    val outputStream = MemoryPooledByteBufferOutputStream(pool, initialCapacity)
    return try {
      newByteBuf(inputStream, outputStream)
    } finally {
      outputStream.close()
    }
  }

  /**
   * Reads all bytes from inputStream and writes them to outputStream. When all bytes are read
   * outputStream.toByteBuffer is called and obtained MemoryPooledByteBuffer is returned
   *
   * @param inputStream the input stream to read from
   * @param outputStream output stream used to transform content of input stream to
   *   MemoryPooledByteBuffer
   * @return an instance of MemoryPooledByteBuffer
   * @throws IOException
   */
  @VisibleForTesting
  @Throws(IOException::class)
  fun newByteBuf(
      inputStream: InputStream,
      outputStream: MemoryPooledByteBufferOutputStream
  ): MemoryPooledByteBuffer {
    pooledByteStreams.copy(inputStream, outputStream)
    return outputStream.toByteBuffer()
  }

  override fun newOutputStream(): MemoryPooledByteBufferOutputStream =
      MemoryPooledByteBufferOutputStream(pool)

  override fun newOutputStream(initialCapacity: Int): MemoryPooledByteBufferOutputStream =
      MemoryPooledByteBufferOutputStream(pool, initialCapacity)
}
