/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Arrays
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PooledByteStreamsTest {
  companion object {
    private const val POOLED_ARRAY_SIZE = 4
  }

  private lateinit var byteArrayPool: ByteArrayPool
  private lateinit var pooledArray: ByteArray

  private lateinit var data: ByteArray
  private lateinit var inputStream: InputStream
  private lateinit var outputStream: ByteArrayOutputStream

  private lateinit var pooledByteStreams: PooledByteStreams

  @Before
  fun setUp() {
    byteArrayPool = Mockito.mock(ByteArrayPool::class.java)
    data = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 115)
    inputStream = ByteArrayInputStream(data)
    outputStream = ByteArrayOutputStream()

    pooledArray = ByteArray(4)
    pooledByteStreams = PooledByteStreams(byteArrayPool, POOLED_ARRAY_SIZE)
    Mockito.`when`(byteArrayPool.get(POOLED_ARRAY_SIZE)).thenReturn(pooledArray)
  }

  @Test
  @Throws(IOException::class)
  fun testUsesPool() {
    pooledByteStreams.copy(inputStream, outputStream)
    Mockito.verify(byteArrayPool).get(POOLED_ARRAY_SIZE)
    Mockito.verify(byteArrayPool).release(pooledArray)
  }

  @Test
  @Throws(IOException::class)
  fun testReleasesOnException() {
    try {
      pooledByteStreams.copy(
          inputStream,
          object : OutputStream() {
            @Throws(IOException::class)
            override fun write(oneByte: Int) {
              throw IOException()
            }
          })
      Assertions.fail("Expected IOException was not thrown")
    } catch (ioe: IOException) {
      // expected
    }

    Mockito.verify(byteArrayPool).release(pooledArray)
  }

  @Test
  @Throws(IOException::class)
  fun testCopiesData() {
    pooledByteStreams.copy(inputStream, outputStream)
    Assertions.assertThat(outputStream.toByteArray()).isEqualTo(data)
  }

  @Test
  @Throws(IOException::class)
  fun testReleasesOnExceptionWithSize() {
    try {
      pooledByteStreams.copy(
          inputStream,
          object : OutputStream() {
            @Throws(IOException::class)
            override fun write(oneByte: Int) {
              throw IOException()
            }
          },
          3)
      Assertions.fail("Expected IOException was not thrown")
    } catch (ioe: IOException) {
      // expected
    }

    Mockito.verify(byteArrayPool).release(pooledArray)
  }

  @Test
  @Throws(IOException::class)
  fun testCopiesDataWithSize() {
    pooledByteStreams.copy(inputStream, outputStream, 3)
    Assertions.assertThat(outputStream.toByteArray()).isEqualTo(Arrays.copyOf(data, 3))
  }
}
