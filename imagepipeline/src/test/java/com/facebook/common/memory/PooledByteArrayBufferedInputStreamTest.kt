/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory

import com.facebook.common.references.ResourceReleaser
import java.io.ByteArrayInputStream
import java.io.IOException
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PooledByteArrayBufferedInputStreamTest {

  private lateinit var resourceReleaser: ResourceReleaser<ByteArray>
  private lateinit var buffer: ByteArray
  private lateinit var pooledByteArrayBufferedInputStream: PooledByteArrayBufferedInputStream

  @Before
  fun setUp() {
    @Suppress("UNCHECKED_CAST")
    resourceReleaser = Mockito.mock(ResourceReleaser::class.java) as ResourceReleaser<ByteArray>
    val bytes = ByteArray(256)
    for (i in 0 until 256) {
      bytes[i] = i.toByte()
    }
    val unbufferedStream = ByteArrayInputStream(bytes)
    buffer = ByteArray(10)
    pooledByteArrayBufferedInputStream =
        PooledByteArrayBufferedInputStream(unbufferedStream, buffer, resourceReleaser)
  }

  @Test
  @Throws(IOException::class)
  fun testSingleByteRead() {
    for (i in 0 until 256) {
      Assertions.assertThat(pooledByteArrayBufferedInputStream.read()).isEqualTo(i)
    }
    Assertions.assertThat(pooledByteArrayBufferedInputStream.read()).isEqualTo(-1)
  }

  @Test
  @Throws(IOException::class)
  fun testReleaseOnClose() {
    pooledByteArrayBufferedInputStream.close()
    Mockito.verify(resourceReleaser).release(buffer)
    pooledByteArrayBufferedInputStream.close()
    // we do not expect second close to release resource again,
    // the one checked bellow is the one that happened when close was called for the first time
    Mockito.verify(resourceReleaser, Mockito.times(1)).release(Mockito.any(ByteArray::class.java))
  }

  @Test
  @Throws(IOException::class)
  fun testSkip() {
    // buffer some data
    pooledByteArrayBufferedInputStream.read()
    Assertions.assertThat(pooledByteArrayBufferedInputStream.skip(99)).isEqualTo(99)
    Assertions.assertThat(pooledByteArrayBufferedInputStream.read()).isEqualTo(100)
  }

  @Test
  @Throws(IOException::class)
  fun testSkip2() {
    var i = 0
    while (i < 256) {
      Assertions.assertThat(pooledByteArrayBufferedInputStream.read()).isEqualTo(i)
      i += (pooledByteArrayBufferedInputStream.skip(7) + 1).toInt()
    }
  }

  @Test
  fun testMark() {
    Assertions.assertThat(pooledByteArrayBufferedInputStream.markSupported()).isFalse()
  }

  @Test
  @Throws(IOException::class)
  fun testReadWithByteArray() {
    val readBuffer = ByteArray(5)
    Assertions.assertThat(pooledByteArrayBufferedInputStream.read(readBuffer)).isEqualTo(5)
    assertFilledWithConsecutiveBytes(readBuffer, 0, 5, 0)
  }

  @Test
  @Throws(IOException::class)
  fun testNonFullRead() {
    val readBuffer = ByteArray(200)
    Assertions.assertThat(pooledByteArrayBufferedInputStream.read(readBuffer)).isEqualTo(10)
    assertFilledWithConsecutiveBytes(readBuffer, 0, 10, 0)
    assertFilledWithZeros(readBuffer, 10, 200)
  }

  @Test
  @Throws(IOException::class)
  fun testNonFullReadWithOffset() {
    val readBuffer = ByteArray(200)
    Assertions.assertThat(pooledByteArrayBufferedInputStream.read(readBuffer, 45, 75)).isEqualTo(10)
    assertFilledWithZeros(readBuffer, 0, 45)
    assertFilledWithConsecutiveBytes(readBuffer, 45, 55, 0)
    assertFilledWithZeros(readBuffer, 55, 200)
  }

  @Test
  @Throws(IOException::class)
  fun testReadsCombined() {
    val readBuffer = ByteArray(5)
    var i = 0
    while (i <= 245) {
      Assertions.assertThat(pooledByteArrayBufferedInputStream.read()).isEqualTo(i)

      Assertions.assertThat(pooledByteArrayBufferedInputStream.read(readBuffer)).isEqualTo(5)
      assertFilledWithConsecutiveBytes(readBuffer, 0, readBuffer.size, i + 1)

      Assertions.assertThat(pooledByteArrayBufferedInputStream.read(readBuffer, 1, 3)).isEqualTo(3)
      Assertions.assertThat(readBuffer[0]).isEqualTo((i + 1).toByte())
      assertFilledWithConsecutiveBytes(readBuffer, 1, 4, i + 6)
      Assertions.assertThat(readBuffer[4]).isEqualTo((i + 5).toByte())

      Assertions.assertThat(pooledByteArrayBufferedInputStream.skip(2)).isEqualTo(2)

      i += 11
    }

    Assertions.assertThat(pooledByteArrayBufferedInputStream.available()).isEqualTo(256 - i)
  }

  /**
   * Given byte array, asserts that bytes in [startOffset, endOffset) range are all zeroed.
   *
   * @param byteArray The byte array to check
   * @param startOffset The start offset (inclusive)
   * @param endOffset The end offset (exclusive)
   */
  private fun assertFilledWithZeros(byteArray: ByteArray, startOffset: Int, endOffset: Int) {
    for (i in startOffset until endOffset) {
      Assertions.assertThat(byteArray[i]).isEqualTo(0.toByte())
    }
  }

  /**
   * Given byte array, asserts that each byte in (startOffset, endOffset) range has value equal to
   * value of previous byte plus one (mod 255) and byteArray[startOffset] is equal to firstByte.
   *
   * @param byteArray The byte array to check
   * @param startOffset The start offset (inclusive)
   * @param endOffset The end offset (exclusive)
   * @param firstByte The expected value of the first byte
   */
  private fun assertFilledWithConsecutiveBytes(
      byteArray: ByteArray,
      startOffset: Int,
      endOffset: Int,
      firstByte: Int
  ) {
    var expectedByte = firstByte
    for (i in startOffset until endOffset) {
      Assertions.assertThat(byteArray[i]).isEqualTo(expectedByte.toByte())
      expectedByte++
    }
  }
}
