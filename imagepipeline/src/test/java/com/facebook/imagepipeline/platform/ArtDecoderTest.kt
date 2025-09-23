/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import androidx.core.util.Pools
import com.facebook.common.internal.ByteStreams
import com.facebook.common.internal.Throwables
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.memory.BitmapPool
import com.facebook.imagepipeline.testing.MockBitmapFactory
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import com.facebook.imageutils.JfifUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [ArtDecoder]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class ArtDecoderTest {
  private lateinit var bitmapPool: BitmapPool
  private lateinit var pooledByteBuffer: PooledByteBuffer
  private lateinit var byteBufferRef: CloseableReference<PooledByteBuffer>
  private lateinit var bitmapRegionDecoder: BitmapRegionDecoder
  private lateinit var artDecoder: ArtDecoder
  private lateinit var bitmap: Bitmap
  private lateinit var encodedImage: EncodedImage
  private lateinit var encodedBytes: ByteArray
  private var tempStorage: ByteArray? = null

  @Before
  @Throws(Exception::class)
  fun setUp() {
    val random = Random()
    random.setSeed(RANDOM_SEED.toLong())
    encodedBytes = ByteArray(ENCODED_BYTES_LENGTH)
    random.nextBytes(encodedBytes)

    pooledByteBuffer = TrivialPooledByteBuffer(encodedBytes)
    bitmapPool = mock<BitmapPool>()
    val pool: Pools.SynchronizedPool<ByteBuffer> = Pools.SynchronizedPool<ByteBuffer>(1)
    pool.release(ByteBuffer.allocate(16 * 1024))
    artDecoder = ArtDecoder(bitmapPool, pool, PlatformDecoderOptions())

    byteBufferRef = CloseableReference.of(pooledByteBuffer)
    encodedImage = EncodedImage(byteBufferRef)
    encodedImage.setImageFormat(DefaultImageFormats.JPEG)
    bitmap = MockBitmapFactory.create()
    doReturn(bitmap).whenever(bitmapPool).get(MockBitmapFactory.DEFAULT_BITMAP_SIZE)

    bitmapRegionDecoder = mock<BitmapRegionDecoder>()

    val buf: ByteBuffer? = artDecoder.mDecodeBuffers.acquire()
    tempStorage = buf?.array()
    buf?.let { artDecoder.mDecodeBuffers.release(it) }
  }

  @After
  fun tearDown() {
    // Clean up resources if needed
  }

  @Test
  fun testDecodeStaticDecodesFromStream() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      setupBitmapFactoryMocks(mockedBitmapFactory)

      artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, null)
      verifyDecodedFromStream(mockedBitmapFactory)
    }
  }

  @Test
  fun testDecodeStaticDoesNotLeak() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      setupBitmapFactoryMocks(mockedBitmapFactory)

      artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, null)
      verifyNoLeaks()
    }
  }

  @Test
  fun testStaticImageUsesPooledByteBufferWithPixels() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      setupBitmapFactoryMocks(mockedBitmapFactory)

      val decodedImage: CloseableReference<Bitmap>? =
          artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, null)
      closeAndVerifyClosed(decodedImage)
    }
  }

  @Test(expected = NullPointerException::class)
  fun testPoolsReturnsNull() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      setupBitmapFactoryMocks(mockedBitmapFactory)
      doReturn(null).whenever(bitmapPool).get(any<Int>())
      artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, null)
    }
  }

  @Test(expected = IllegalStateException::class)
  fun testBitmapFactoryReturnsNewBitmap() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      // Setup mock to return null for bounds check, then a different bitmap
      whenever(
              BitmapFactory.decodeStream(any<InputStream>(), isNull(), any<BitmapFactory.Options>())
          )
          .thenAnswer { invocation ->
            val options = invocation.getArgument<BitmapFactory.Options>(2)
            options.outWidth = MockBitmapFactory.DEFAULT_BITMAP_WIDTH
            options.outHeight = MockBitmapFactory.DEFAULT_BITMAP_HEIGHT
            if (options.inJustDecodeBounds) {
              null
            } else {
              // Return a different bitmap than expected to trigger IllegalStateException
              MockBitmapFactory.create()
            }
          }

      artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, null)
    }
  }

  @Test(expected = RuntimeException::class)
  fun testBitmapFactoryThrowsAnException() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      // Setup mock to return null for bounds check, then throw exception
      whenever(
              BitmapFactory.decodeStream(any<InputStream>(), isNull(), any<BitmapFactory.Options>())
          )
          .thenAnswer { invocation ->
            val options = invocation.getArgument<BitmapFactory.Options>(2)
            options.outWidth = MockBitmapFactory.DEFAULT_BITMAP_WIDTH
            options.outHeight = MockBitmapFactory.DEFAULT_BITMAP_HEIGHT
            if (options.inJustDecodeBounds) {
              null
            } else {
              // Throw exception on actual decode
              throw RuntimeException("Test exception")
            }
          }

      artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, null)
    }
  }

  @Test
  fun testDecodeJpeg_allBytes_complete() {
    jpegTestCase(true, ENCODED_BYTES_LENGTH)
  }

  @Test
  fun testDecodeJpeg_notAllBytes_complete() {
    jpegTestCase(true, ENCODED_BYTES_LENGTH / 2)
  }

  @Test
  fun testDecodeJpeg_allBytes_incomplete() {
    jpegTestCase(false, ENCODED_BYTES_LENGTH)
  }

  @Test
  fun testDecodeJpeg_notAllBytes_incomplete() {
    jpegTestCase(false, ENCODED_BYTES_LENGTH / 2)
  }

  @Test
  fun testDecodeJpeg_regionDecodingEnabled() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      Mockito.mockStatic(BitmapRegionDecoder::class.java).use { mockedBitmapRegionDecoder ->
        setupBitmapFactoryMocks(mockedBitmapFactory)

        val region = Rect(0, 0, 200, 100)
        val size: Int =
            MockBitmapFactory.bitmapSize(region.width(), region.height(), DEFAULT_BITMAP_CONFIG)

        val regionBitmap: Bitmap =
            MockBitmapFactory.create(region.width(), region.height(), DEFAULT_BITMAP_CONFIG)

        whenever(bitmapRegionDecoder.decodeRegion(any<Rect>(), any<BitmapFactory.Options>()))
            .thenReturn(regionBitmap)

        whenever(BitmapRegionDecoder.newInstance(any<InputStream>(), any<Boolean>()))
            .thenReturn(bitmapRegionDecoder)

        doReturn(regionBitmap).whenever(bitmapPool).get(size)
        val decodedImage: CloseableReference<Bitmap>? =
            artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, region)

        assertThat(decodedImage?.get()?.width).isEqualTo(region.width())
        assertThat(decodedImage?.get()?.height).isEqualTo(region.height())
        closeAndVerifyClosed(decodedImage, regionBitmap)
        verify(bitmapRegionDecoder).recycle()
      }
    }
  }

  @Test
  fun testDecodeFromEncodedImage_regionDecodingEnabled() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      Mockito.mockStatic(BitmapRegionDecoder::class.java).use { mockedBitmapRegionDecoder ->
        setupBitmapFactoryMocks(mockedBitmapFactory)

        val region = Rect(0, 0, 200, 100)
        val size: Int =
            MockBitmapFactory.bitmapSize(region.width(), region.height(), DEFAULT_BITMAP_CONFIG)

        val regionBitmap: Bitmap =
            MockBitmapFactory.create(region.width(), region.height(), DEFAULT_BITMAP_CONFIG)

        whenever(bitmapRegionDecoder.decodeRegion(any<Rect>(), any<BitmapFactory.Options>()))
            .thenReturn(regionBitmap)

        whenever(BitmapRegionDecoder.newInstance(any<InputStream>(), any<Boolean>()))
            .thenReturn(bitmapRegionDecoder)

        doReturn(regionBitmap).whenever(bitmapPool).get(size)
        val decodedImage: CloseableReference<Bitmap>? =
            artDecoder.decodeFromEncodedImage(encodedImage, DEFAULT_BITMAP_CONFIG, region)

        assertThat(decodedImage?.get()?.width).isEqualTo(region.width())
        assertThat(decodedImage?.get()?.height).isEqualTo(region.height())
        closeAndVerifyClosed(decodedImage, regionBitmap)
        verify(bitmapRegionDecoder).recycle()
      }
    }
  }

  private fun jpegTestCase(complete: Boolean, dataLength: Int) {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      setupBitmapFactoryMocks(mockedBitmapFactory)

      if (complete) {
        encodedBytes[dataLength - 2] = JfifUtil.MARKER_FIRST_BYTE.toByte()
        encodedBytes[dataLength - 1] = JfifUtil.MARKER_EOI.toByte()
      }
      val result: CloseableReference<Bitmap>? =
          artDecoder.decodeJPEGFromEncodedImage(
              encodedImage,
              DEFAULT_BITMAP_CONFIG,
              null,
              dataLength,
          )
      verifyDecodedFromStream(mockedBitmapFactory)
      verifyNoLeaks()
      verifyDecodedBytes(complete, dataLength, mockedBitmapFactory)
      closeAndVerifyClosed(result)
    }
  }

  private fun setupBitmapFactoryMocks(
      mockedBitmapFactory: org.mockito.MockedStatic<BitmapFactory>
  ) {
    whenever(BitmapFactory.decodeStream(any<InputStream>(), isNull(), any<BitmapFactory.Options>()))
        .thenAnswer { invocation ->
          val options = invocation.getArgument<BitmapFactory.Options>(2)
          options.outWidth = MockBitmapFactory.DEFAULT_BITMAP_WIDTH
          options.outHeight = MockBitmapFactory.DEFAULT_BITMAP_HEIGHT
          verifyBitmapFactoryOptions(options)
          if (options.inJustDecodeBounds) null else bitmap
        }
  }

  private fun verifyBitmapFactoryOptions(options: BitmapFactory.Options) {
    if (!options.inJustDecodeBounds) {
      assertThat(options.inDither).isTrue()
      assertThat(options.inMutable).isTrue()
      assertThat(options.inBitmap).isNotNull()
      assertThat(options.inTempStorage).isEqualTo(tempStorage)
      val inBitmapWidth = options.inBitmap?.width ?: 0
      val inBitmapHeight = options.inBitmap?.height ?: 0
      assertThat(inBitmapWidth * inBitmapHeight)
          .isGreaterThanOrEqualTo(MockBitmapFactory.DEFAULT_BITMAP_PIXELS)
    }
  }

  private fun closeAndVerifyClosed(closeableImage: CloseableReference<Bitmap>?) {
    verify(bitmapPool, never()).release(bitmap)
    closeableImage?.close()
    verify(bitmapPool).release(bitmap)
  }

  private fun closeAndVerifyClosed(
      closeableImage: CloseableReference<Bitmap>?,
      expectedBitmap: Bitmap,
  ) {
    verify(bitmapPool, never()).release(expectedBitmap)
    closeableImage?.close()
    verify(bitmapPool).release(expectedBitmap)
  }

  private fun verifyNoLeaks() {
    assertThat(byteBufferRef.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
  }

  private fun verifyDecodedBytes(
      complete: Boolean,
      length: Int,
      mockedBitmapFactory: org.mockito.MockedStatic<BitmapFactory>,
  ) {
    val decodedBytes: ByteArray = getDecodedBytes(mockedBitmapFactory)
    assertThat(decodedBytes.copyOfRange(0, length)).isEqualTo(encodedBytes.copyOfRange(0, length))
    if (complete) {
      assertThat(decodedBytes.size).isEqualTo(length)
    } else {
      assertThat(decodedBytes.size).isEqualTo(length + 2)
      assertThat(decodedBytes[length]).isEqualTo(JfifUtil.MARKER_FIRST_BYTE.toByte())
      assertThat(decodedBytes[length + 1]).isEqualTo(JfifUtil.MARKER_EOI.toByte())
    }
  }

  private fun getDecodedBytes(
      mockedBitmapFactory: org.mockito.MockedStatic<BitmapFactory>
  ): ByteArray {
    val inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream::class.java)
    mockedBitmapFactory.verify(
        {
          BitmapFactory.decodeStream(
              inputStreamArgumentCaptor.capture(),
              isNull<Rect>(),
              any<BitmapFactory.Options>(),
          )
        },
        times(2),
    )
    val decodedStream = inputStreamArgumentCaptor.value
    val baos = ByteArrayOutputStream()
    try {
      ByteStreams.copy(decodedStream, baos)
    } catch (ioe: IOException) {
      throw Throwables.propagate(ioe)
    }
    return baos.toByteArray()
  }

  private fun verifyDecodedFromStream(
      mockedBitmapFactory: org.mockito.MockedStatic<BitmapFactory>
  ) {
    mockedBitmapFactory.verify(
        {
          BitmapFactory.decodeStream(
              any<InputStream>(),
              isNull<Rect>(),
              any<BitmapFactory.Options>(),
          )
        },
        times(2),
    )
  }

  companion object {
    private val DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private const val RANDOM_SEED = 10101
    private const val ENCODED_BYTES_LENGTH = 128
  }
}
