/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory

import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [PooledByteBufferInputStream] */
@RunWith(RobolectricTestRunner::class)
class PooledByteBufferInputStreamTest {
  companion object {
    private val BYTES = byteArrayOf(1, 123, -20, 3, 6, 23, 1)
  }

  private lateinit var stream: PooledByteBufferInputStream

  @Before
  fun setup() {
    val buffer = TrivialPooledByteBuffer(BYTES)
    stream = PooledByteBufferInputStream(buffer)
  }

  @Test
  fun testBasic() {
    Assertions.assertThat(stream.mOffset).isEqualTo(0)
    Assertions.assertThat(stream.mMark).isEqualTo(0)
    Assertions.assertThat(stream.available()).isEqualTo(BYTES.size)
    Assertions.assertThat(stream.markSupported()).isTrue()
  }

  @Test
  fun testMark() {
    stream.skip(2)
    stream.mark(0)
    Assertions.assertThat(stream.mMark).isEqualTo(2)
    stream.read()
    Assertions.assertThat(stream.mMark).isEqualTo(2)
    stream.mark(0)
    Assertions.assertThat(stream.mMark).isEqualTo(3)
  }

  @Test
  fun testReset() {
    stream.skip(2)
    stream.reset()
    Assertions.assertThat(stream.mOffset).isEqualTo(0)
  }

  @Test
  fun testAvailable() {
    Assertions.assertThat(stream.available()).isEqualTo(BYTES.size)
    stream.skip(3)
    Assertions.assertThat(stream.available()).isEqualTo(BYTES.size - 3)
    stream.skip(BYTES.size.toLong())
    Assertions.assertThat(stream.available()).isEqualTo(0)
  }

  @Test
  fun testSkip() {
    Assertions.assertThat(stream.skip(2)).isEqualTo(2)
    Assertions.assertThat(stream.mOffset).isEqualTo(2)

    Assertions.assertThat(stream.skip(3)).isEqualTo(3)
    Assertions.assertThat(stream.mOffset).isEqualTo(5)

    // After skipping 5 bytes, only 2 bytes remain (BYTES.size = 7)
    Assertions.assertThat(stream.skip(BYTES.size.toLong())).isEqualTo(2)
    // Now we've skipped all bytes, so mOffset should be 7
    Assertions.assertThat(stream.mOffset).isEqualTo(7)
    // Trying to skip more should return 0
    Assertions.assertThat(stream.skip(BYTES.size.toLong())).isEqualTo(0)
    // mOffset should still be 7
    Assertions.assertThat(stream.mOffset).isEqualTo(7)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testSkipNegative() {
    stream.skip(-4)
  }

  @Test(expected = ArrayIndexOutOfBoundsException::class)
  fun testReadWithErrors() {
    stream.read(ByteArray(64), 10, 55)
  }

  @Test
  fun testRead_SingleByte() {
    for (i in BYTES.indices) {
      Assertions.assertThat(stream.read()).isEqualTo(BYTES[i].toInt() and 0xFF)
    }
    Assertions.assertThat(stream.read()).isEqualTo(-1)
  }

  @Test
  fun testRead_ToByteArray() {
    val buf = ByteArray(64)

    Assertions.assertThat(stream.read(buf, 0, 0)).isEqualTo(0)
    Assertions.assertThat(stream.mOffset).isEqualTo(0)

    Assertions.assertThat(stream.read(buf, 0, 3)).isEqualTo(3)
    Assertions.assertThat(stream.mOffset).isEqualTo(3)
    assertArrayEquals(BYTES, buf, 3)
    for (i in 3 until buf.size) {
      Assertions.assertThat(buf[i]).isEqualTo(0)
    }

    val available = BYTES.size - stream.mOffset
    Assertions.assertThat(stream.read(buf, 3, available + 1)).isEqualTo(available)
    Assertions.assertThat(stream.mOffset).isEqualTo(BYTES.size)
    assertArrayEquals(BYTES, buf, available)

    Assertions.assertThat(stream.read(buf, 0, 1)).isEqualTo(-1)
    Assertions.assertThat(stream.mOffset).isEqualTo(BYTES.size)
  }

  @Test
  fun testRead_ToByteArray2() {
    val buf = ByteArray(BYTES.size + 10)
    Assertions.assertThat(stream.read(buf)).isEqualTo(BYTES.size)
    assertArrayEquals(BYTES, buf, BYTES.size)
  }

  @Test
  fun testRead_ToByteArray3() {
    val buf = ByteArray(BYTES.size - 1)
    Assertions.assertThat(stream.read(buf)).isEqualTo(buf.size)
    Assertions.assertThat(stream.mOffset).isEqualTo(buf.size)
    assertArrayEquals(BYTES, buf, buf.size)
  }

  @Test
  @Throws(Exception::class)
  fun testCreateEmptyStream() {
    val emptyBuffer = TrivialPooledByteBuffer(ByteArray(0))
    val is1 = PooledByteBufferInputStream(emptyBuffer)
    Assertions.assertThat(is1.read()).isEqualTo(-1)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testCreatingStreamAfterClose() {
    val buffer = TrivialPooledByteBuffer(ByteArray(0))
    buffer.close()
    PooledByteBufferInputStream(buffer)
  }

  // assert that the first 'length' bytes of expected are the same as those in 'actual'
  private fun assertArrayEquals(expected: ByteArray, actual: ByteArray, length: Int) {
    Assertions.assertThat(expected.size >= length).isTrue()
    Assertions.assertThat(actual.size >= length).isTrue()
    for (i in 0 until length) {
      Assertions.assertThat(actual[i]).isEqualTo(expected[i])
    }
  }
}
