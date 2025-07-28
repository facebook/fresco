/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder

import com.facebook.common.internal.ByteStreams
import com.facebook.common.memory.ByteArrayPool
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgressiveJpegParserTest {

  @Mock lateinit var resourceReleaser: ResourceReleaser<*>

  private lateinit var progressiveJpegParser: ProgressiveJpegParser
  private lateinit var webpBytes: ByteArray
  private lateinit var partialWebpBytes: ByteArray
  private lateinit var jpegBytes: ByteArray

  @Before
  @Throws(IOException::class)
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    val byteArrayPool = mock<ByteArrayPool>()
    whenever(byteArrayPool.get(any<Int>())).thenReturn(ByteArray(10))
    progressiveJpegParser = ProgressiveJpegParser(byteArrayPool)

    jpegBytes =
        ByteStreams.toByteArray(
            requireNotNull(
                ProgressiveJpegParserTest::class.java.getResourceAsStream("images/image.jpg")) {
                  "Could not find test resource: images/image.jpg"
                })
    webpBytes =
        ByteStreams.toByteArray(
            requireNotNull(
                ProgressiveJpegParserTest::class.java.getResourceAsStream("images/image.webp")) {
                  "Could not find test resource: images/image.webp"
                })
    partialWebpBytes = ByteArray(webpBytes.size / 2)
    System.arraycopy(webpBytes, 0, partialWebpBytes, 0, partialWebpBytes.size)
  }

  @Test
  fun testOnPartialWebp() {
    val byteBuffer = TrivialPooledByteBuffer(partialWebpBytes)
    progressiveJpegParser.parseMoreData(buildEncodedImage(byteBuffer))
    assertThat(progressiveJpegParser.isJpeg).isFalse()
  }

  @Test
  fun testOnWebp() {
    val byteBuffer = TrivialPooledByteBuffer(webpBytes)
    progressiveJpegParser.parseMoreData(buildEncodedImage(byteBuffer))
    assertThat(progressiveJpegParser.isJpeg).isFalse()
  }

  @Test
  fun testOnTooShortImage() {
    val shortByteBuffer = TrivialPooledByteBuffer(byteArrayOf(0xff.toByte()))
    assertThat(progressiveJpegParser.isJpeg).isFalse()
    assertThat(progressiveJpegParser.parseMoreData(buildEncodedImage(shortByteBuffer))).isFalse()
    assertThat(progressiveJpegParser.isJpeg).isFalse()
    assertThat(progressiveJpegParser.bestScanEndOffset).isEqualTo(0)
    assertThat(progressiveJpegParser.bestScanNumber).isEqualTo(0)
  }

  @Test
  fun testOnShortestJpeg() {
    val shortByteBuffer = TrivialPooledByteBuffer(byteArrayOf(0xff.toByte(), 0xd8.toByte()))
    assertThat(progressiveJpegParser.parseMoreData(buildEncodedImage(shortByteBuffer))).isFalse()
    assertThat(progressiveJpegParser.isJpeg).isTrue()
    assertThat(progressiveJpegParser.bestScanEndOffset).isEqualTo(0)
    assertThat(progressiveJpegParser.bestScanNumber).isEqualTo(0)
  }

  @Test
  fun testBasic() {
    val veryFakeJpeg =
        byteArrayOf(
            0xff.toByte(),
            0xd8.toByte(),
            0xff.toByte(),
            0xff.toByte(),
            0xff.toByte(),
            0xda.toByte(),
            0x00.toByte(),
            0x03.toByte(),
            0x00.toByte(),
            0xff.toByte(),
            0xff.toByte(),
            0xff.toByte(),
            0xda.toByte(),
            0x00.toByte(),
            0x03.toByte(),
            0x00.toByte(),
            0xff.toByte(),
            0xda.toByte(),
            0x00.toByte(),
            0x03.toByte(),
            0x00.toByte(),
            0xff.toByte(),
            0xda.toByte())

    testFirstNBytes(veryFakeJpeg, 3, false, 0, 0)
    testFirstNBytes(veryFakeJpeg, 6, false, 0, 0)
    testFirstNBytes(veryFakeJpeg, 8, false, 0, 0)
    testFirstNBytes(veryFakeJpeg, 13, true, 1, 11)
    testFirstNBytes(veryFakeJpeg, 13, false, 1, 11)
    testFirstNBytes(veryFakeJpeg, 17, false, 1, 11)
    testFirstNBytes(veryFakeJpeg, 18, true, 2, 16)
    testFirstNBytes(veryFakeJpeg, 20, false, 2, 16)
    testFirstNBytes(veryFakeJpeg, veryFakeJpeg.size, true, 3, 21)
  }

  @Test
  fun testOnRealJpeg() {
    testFirstNBytes(jpegBytes, 7000, true, 1, 4332)
    testFirstNBytes(jpegBytes, jpegBytes.size, true, 10, 32844)
  }

  /**
   * Feeds progressiveJpegParser with n initial bytes from byteArray and checks that
   *
   * @param byteArray
   * @param n
   * @param foundNewScan expected return value of ProgressiveJpegParser.parseMoreData
   * @param expectedBestScan expected number of scans found by the parser + 1
   * @param bestScanEndOffset offset of expected best scan found so far
   */
  private fun testFirstNBytes(
      byteArray: ByteArray,
      n: Int,
      foundNewScan: Boolean,
      expectedBestScan: Int,
      bestScanEndOffset: Int
  ) {
    assertThat(
            progressiveJpegParser.parseMoreData(
                buildEncodedImage(TrivialPooledByteBuffer(byteArray.copyOf(n)))))
        .isEqualTo(foundNewScan)
    assertThat(progressiveJpegParser.isJpeg).isTrue()
    assertThat(progressiveJpegParser.bestScanNumber).isEqualTo(expectedBestScan)
    assertThat(progressiveJpegParser.bestScanEndOffset).isEqualTo(bestScanEndOffset)
  }

  companion object {
    private fun buildEncodedImage(byteBuffer: TrivialPooledByteBuffer): EncodedImage {
      return EncodedImage(CloseableReference.of<PooledByteBuffer>(byteBuffer))
    }
  }
}
