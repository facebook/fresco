/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import com.facebook.common.internal.ByteStreams
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.common.BytesRange
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import com.facebook.imageutils.JfifUtil
import java.io.FileInputStream
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/** Tests for [EncodedImage] */
@RunWith(RobolectricTestRunner::class)
class EncodedImageTest {
  private lateinit var byteBufferRef: CloseableReference<PooledByteBuffer>
  private lateinit var inputStream: FileInputStream
  private lateinit var inputStreamSupplier: Supplier<FileInputStream>

  @Before
  fun setup() {
    val pooledByteBuffer = mock<PooledByteBuffer>()
    byteBufferRef = CloseableReference.of(pooledByteBuffer)
    inputStream = mock<FileInputStream>()
    inputStreamSupplier =
        object : Supplier<FileInputStream> {
          override fun get(): FileInputStream {
            return inputStream
          }
        }
  }

  @Test
  fun testByteBufferRef() {
    val encodedImage = EncodedImage(byteBufferRef)
    assertThat(byteBufferRef.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    assertThat(encodedImage.byteBufferRef?.underlyingReferenceTestOnly)
        .isSameAs(byteBufferRef.underlyingReferenceTestOnly)
  }

  @Test
  fun testInputStream() {
    val encodedImage = EncodedImage(inputStreamSupplier)
    assertThat(encodedImage.inputStream).isSameAs(inputStreamSupplier.get())
  }

  @Test
  fun testCloneOrNull() {
    var encodedImage = EncodedImage(byteBufferRef)
    encodedImage.imageFormat = DefaultImageFormats.JPEG
    encodedImage.rotationAngle = 0
    encodedImage.exifOrientation = 5
    encodedImage.width = 1
    encodedImage.height = 2
    encodedImage.sampleSize = 4
    encodedImage.bytesRange = BytesRange.toMax(1000)
    var encodedImage2 = EncodedImage.cloneOrNull(encodedImage)
    assertThat(byteBufferRef.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(3)
    assertThat(encodedImage.byteBufferRef?.underlyingReferenceTestOnly)
        .isSameAs(encodedImage2?.byteBufferRef?.underlyingReferenceTestOnly)
    assertThat(encodedImage.imageFormat).isEqualTo(encodedImage2?.imageFormat)
    assertThat(encodedImage.rotationAngle).isEqualTo(encodedImage2?.rotationAngle)
    assertThat(encodedImage.exifOrientation).isEqualTo(encodedImage2?.exifOrientation)
    assertThat(encodedImage.height).isEqualTo(encodedImage2?.height)
    assertThat(encodedImage.width).isEqualTo(encodedImage2?.width)
    assertThat(encodedImage.sampleSize).isEqualTo(encodedImage2?.sampleSize)
    assertThat(encodedImage.bytesRange).isEqualTo(encodedImage2?.bytesRange)

    encodedImage = EncodedImage(inputStreamSupplier, 100)
    encodedImage.imageFormat = DefaultImageFormats.JPEG
    encodedImage.rotationAngle = 0
    encodedImage.exifOrientation = 5
    encodedImage.width = 1
    encodedImage.height = 2
    encodedImage2 = EncodedImage.cloneOrNull(encodedImage)
    assertThat(encodedImage.inputStream).isSameAs(encodedImage2?.inputStream)
    assertThat(encodedImage2?.size).isEqualTo(encodedImage.size)
  }

  @Test
  fun testCloneOrNull_withInvalidOrNullReferences() {
    assertThat(EncodedImage.cloneOrNull(null)).isNull()
    val encodedImage = EncodedImage(byteBufferRef)

    encodedImage.close()
    assertThat(EncodedImage.cloneOrNull(encodedImage)).isNull()
  }

  @Test
  fun testClose() {
    val encodedImage = EncodedImage(byteBufferRef)
    encodedImage.close()
    assertThat(byteBufferRef.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(1)
  }

  @Test
  fun testIsValid() {
    var encodedImage = EncodedImage(byteBufferRef)
    assertThat(encodedImage.isValid).isTrue()
    encodedImage.close()
    assertThat(encodedImage.isValid).isFalse()
    encodedImage = EncodedImage(inputStreamSupplier)
    assertThat(encodedImage.isValid).isTrue()
    // Test the static method
    assertThat(EncodedImage.isValid(null)).isFalse()
  }

  @Test
  fun testIsMetaDataAvailable() {
    val encodedImage1 = EncodedImage(byteBufferRef)
    val encodedImage2 = EncodedImage(byteBufferRef)
    encodedImage2.rotationAngle = 1
    encodedImage2.exifOrientation = 1
    encodedImage2.width = 1
    encodedImage2.height = 1
    assertThat(EncodedImage.isMetaDataAvailable(encodedImage1)).isFalse()
    assertThat(EncodedImage.isMetaDataAvailable(encodedImage2)).isTrue()
  }

  @Test
  fun testCloseSafely() {
    val encodedImage = EncodedImage(byteBufferRef)
    EncodedImage.closeSafely(encodedImage)
    assertThat(byteBufferRef.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(1)
  }

  @Test
  fun testGetInputStream() {
    val encodedImage = EncodedImage(inputStreamSupplier)
    assertThat(encodedImage.inputStream).isSameAs(inputStream)
  }

  @Test
  @Throws(IOException::class)
  fun testGetFirstBytesAsHexString() {
    val buf: PooledByteBuffer = TrivialPooledByteBuffer("12345abcd".toByteArray())
    val encodedImage: EncodedImage = EncodedImage(CloseableReference.of(buf))
    assertThat(encodedImage.getFirstBytesAsHexString(9)).isEqualTo("313233343561626364")
    assertThat(encodedImage.getFirstBytesAsHexString(10)).isEqualTo("313233343561626364")
    assertThat(encodedImage.getFirstBytesAsHexString(6)).isEqualTo("313233343561")
  }

  @Test
  @Throws(IOException::class)
  fun testParseMetaData_JPEG() {
    val buf: PooledByteBuffer =
        TrivialPooledByteBuffer(
            ByteStreams.toByteArray(
                EncodedImageTest::class.java.getResourceAsStream("images/image.jpg")!!
            )
        )
    val encodedImage: EncodedImage = EncodedImage(CloseableReference.of(buf))
    encodedImage.parseMetaData()
    assertThat(encodedImage.imageFormat).isSameAs(DefaultImageFormats.JPEG)
    assertThat(encodedImage.width).isEqualTo(550)
    assertThat(encodedImage.height).isEqualTo(468)
    assertThat(encodedImage.rotationAngle).isEqualTo(0)
    assertThat(encodedImage.exifOrientation).isEqualTo(0)
  }

  @Test
  @Throws(IOException::class)
  fun testParseMetaData_PNG() {
    val buf: PooledByteBuffer =
        TrivialPooledByteBuffer(
            ByteStreams.toByteArray(
                EncodedImageTest::class.java.getResourceAsStream("images/image.png")!!
            )
        )
    val encodedImage: EncodedImage = EncodedImage(CloseableReference.of(buf))
    encodedImage.parseMetaData()
    assertThat(encodedImage.imageFormat).isSameAs(DefaultImageFormats.PNG)
    assertThat(encodedImage.width).isEqualTo(800)
    assertThat(encodedImage.height).isEqualTo(600)
    assertThat(encodedImage.rotationAngle).isEqualTo(0)
    assertThat(encodedImage.exifOrientation).isEqualTo(0)
  }

  @Throws(IOException::class)
  private fun checkWebpImage(
      imagePath: String,
      imageFormat: ImageFormat,
      expectedWidth: Int,
      expectedHeight: Int,
  ) {
    val buf: PooledByteBuffer =
        TrivialPooledByteBuffer(
            ByteStreams.toByteArray(EncodedImageTest::class.java.getResourceAsStream(imagePath)!!)
        )
    val encodedImage: EncodedImage = EncodedImage(CloseableReference.of(buf))
    encodedImage.parseMetaData()
    assertThat(encodedImage.imageFormat).isSameAs(imageFormat)
    assertThat(encodedImage.width).isEqualTo(expectedWidth)
    assertThat(encodedImage.height).isEqualTo(expectedHeight)
    assertThat(encodedImage.rotationAngle).isEqualTo(0)
  }

  @Test
  @Throws(IOException::class)
  fun testParseMetaData_SimpleWEBP() {
    checkWebpImage("images/1_webp_plain.webp", DefaultImageFormats.WEBP_SIMPLE, 320, 214)
  }

  @Test
  @Throws(IOException::class)
  fun testParseMetaData_LosslessWEBP() {
    checkWebpImage("images/1_webp_ll.webp", DefaultImageFormats.WEBP_LOSSLESS, 400, 301)
  }

  @Test
  @Throws(IOException::class)
  fun testParseMetaData_ExtendedWithAlphaWEBP() {
    checkWebpImage("images/1_webp_ea.webp", DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA, 400, 301)
  }

  @Test
  @Throws(IOException::class)
  fun testParseMetaData_ExtendedWEBP() {
    checkWebpImage("images/1_webp_e.webp", DefaultImageFormats.WEBP_EXTENDED, 480, 320)
  }

  @Test
  @Throws(IOException::class)
  fun testParseMetaData_AnimatedWEBP() {
    checkWebpImage("images/1_webp_anim.webp", DefaultImageFormats.WEBP_ANIMATED, 322, 477)
  }

  @Test
  fun testIsJpegCompleteAt_notComplete() {
    val encodedBytes = ByteArray(ENCODED_BYTES_LENGTH)
    encodedBytes[ENCODED_BYTES_LENGTH - 2] = 0
    encodedBytes[ENCODED_BYTES_LENGTH - 1] = 0
    val buf: PooledByteBuffer = TrivialPooledByteBuffer(encodedBytes)
    val encodedImage: EncodedImage = EncodedImage(CloseableReference.of(buf))
    encodedImage.imageFormat = DefaultImageFormats.JPEG
    assertThat(encodedImage.isCompleteAt(ENCODED_BYTES_LENGTH)).isFalse()
  }

  @Test
  fun testIsJpegCompleteAt_Complete() {
    val encodedBytes = ByteArray(ENCODED_BYTES_LENGTH)
    encodedBytes[ENCODED_BYTES_LENGTH - 2] = JfifUtil.MARKER_FIRST_BYTE.toByte()
    encodedBytes[ENCODED_BYTES_LENGTH - 1] = JfifUtil.MARKER_EOI.toByte()
    val buf: PooledByteBuffer = TrivialPooledByteBuffer(encodedBytes)
    val encodedImage: EncodedImage = EncodedImage(CloseableReference.of(buf))
    encodedImage.imageFormat = DefaultImageFormats.JPEG
    assertThat(encodedImage.isCompleteAt(ENCODED_BYTES_LENGTH)).isTrue()
  }

  @Test
  fun testCopyMetaData() {
    val encodedImage = EncodedImage(byteBufferRef)
    encodedImage.imageFormat = DefaultImageFormats.JPEG
    encodedImage.rotationAngle = 0
    encodedImage.exifOrientation = 1
    encodedImage.width = 1
    encodedImage.height = 2
    encodedImage.sampleSize = 3
    val encodedImage2 = EncodedImage(byteBufferRef)
    encodedImage2.copyMetaDataFrom(encodedImage)
    assertThat(encodedImage.imageFormat).isEqualTo(encodedImage2.imageFormat)
    assertThat(encodedImage.width).isEqualTo(encodedImage2.width)
    assertThat(encodedImage.height).isEqualTo(encodedImage2.height)
    assertThat(encodedImage.sampleSize).isEqualTo(encodedImage2.sampleSize)
    assertThat(encodedImage.size).isEqualTo(encodedImage2.size)
    assertThat(encodedImage.exifOrientation).isEqualTo(encodedImage2.exifOrientation)

    val encodedImage3 = EncodedImage(inputStreamSupplier)
    encodedImage3.imageFormat = DefaultImageFormats.JPEG
    encodedImage3.rotationAngle = 0
    encodedImage3.exifOrientation = 1
    encodedImage3.width = 1
    encodedImage3.height = 2
    encodedImage3.sampleSize = 3
    val encodedImage4 = EncodedImage(inputStreamSupplier)
    encodedImage4.copyMetaDataFrom(encodedImage3)
    assertThat(encodedImage3.imageFormat).isEqualTo(encodedImage4.imageFormat)
    assertThat(encodedImage3.width).isEqualTo(encodedImage4.width)
    assertThat(encodedImage3.height).isEqualTo(encodedImage4.height)
    assertThat(encodedImage3.sampleSize).isEqualTo(encodedImage4.sampleSize)
    assertThat(encodedImage3.size).isEqualTo(encodedImage4.size)
    assertThat(encodedImage3.exifOrientation).isEqualTo(encodedImage4.exifOrientation)
  }

  companion object {
    private const val ENCODED_BYTES_LENGTH = 100
  }
}
