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
import android.graphics.ColorSpace
import android.graphics.Rect
import android.os.Build
import androidx.core.util.Pools
import com.facebook.common.memory.MemoryTrimType
import com.facebook.imagepipeline.memory.BitmapPool
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DefaultRawBitmapDecoderTest {

  @Test
  fun decodeByteArray_preferred_decodesRequestedSliceWithSampling() {
    val encoded = encodePng(width = 6, height = 4)
    val prefix = byteArrayOf(1, 2, 3)
    val input = prefix + encoded + byteArrayOf(4, 5)
    val decoder = createDecoder(PlatformDecoderOptions(preferByteArrayDecode = true))

    val result =
        decoder.decode(
            input,
            prefix.size,
            encoded.size,
            Bitmap.Config.ARGB_8888,
            2,
            null,
        )

    assertThat(result).isNotNull()
    assertThat(result!!.width).isEqualTo(3)
    assertThat(result.height).isEqualTo(2)
  }

  @Test
  fun decodeByteArray_preferred_appliesDecodeOptionsAndReleasesBuffer() {
    val buffer = ByteBuffer.allocate(128)
    val bufferPool = RecordingBufferPool(buffer)
    val expected = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565)
    val displayP3 = ColorSpace.get(ColorSpace.Named.DISPLAY_P3)
    var observedOptions: BitmapFactory.Options? = null
    val decoder = createDecoder(
        PlatformDecoderOptions(
            decodeImmutableBitmaps = true,
            enableDither = false,
            forceSrgbColorSpace = false,
            preferByteArrayDecode = true,
        ),
        bufferPool,
    )

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeByteArray(
              any<ByteArray>(),
              any<Int>(),
              any<Int>(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenAnswer { invocation ->
            observedOptions = invocation.getArgument(3)
            expected
          }

      val result = decoder.decode(byteArrayOf(1, 2), 0, 2, Bitmap.Config.RGB_565, 3, displayP3)

      assertThat(result).isSameAs(expected)
    }
    assertThat(observedOptions!!.inSampleSize).isEqualTo(3)
    assertThat(observedOptions!!.inPreferredConfig).isEqualTo(Bitmap.Config.RGB_565)
    assertThat(observedOptions!!.inPreferredColorSpace).isEqualTo(displayP3)
    assertThat(observedOptions!!.inDither).isFalse()
    assertThat(observedOptions!!.inMutable).isFalse()
    assertThat(observedOptions!!.inTempStorage).isSameAs(buffer.array())
    assertThat(bufferPool.released).containsExactly(buffer)
  }

  @Test
  fun decodeByteArray_invalidDimensions_reportsEachRejectedBoundary() {
    val dimensions = ArrayDeque(listOf(0 to 1, 1 to 0, 32769 to 1, 1 to 32769))
    val reporter = RecordingErrorReporter()
    val decoder = createDecoder(
        PlatformDecoderOptions(
            enableDecodeDimensionValidation = true,
            errorReporter = reporter,
            preferByteArrayDecode = true,
        ),
    )

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeByteArray(
              any<ByteArray>(),
              any<Int>(),
              any<Int>(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenAnswer { invocation ->
            val (width, height) = dimensions.removeFirst()
            invocation.getArgument<BitmapFactory.Options>(3).also { options ->
              options.outWidth = width
              options.outHeight = height
            }
            null
          }

      repeat(4) {
        assertThat(decoder.decode(byteArrayOf(1), 0, 1, Bitmap.Config.ARGB_8888, 1, null)).isNull()
      }
    }
    assertThat(reporter.reports.map { it.category })
        .containsExactly(
            "DECODE_DIMENSION_VALIDATION",
            "DECODE_DIMENSION_VALIDATION",
            "DECODE_DIMENSION_VALIDATION",
            "DECODE_DIMENSION_VALIDATION",
        )
    assertThat(reporter.reports.map { it.message })
        .containsExactly(
            "Rejecting decode with invalid dimensions: 0x1",
            "Rejecting decode with invalid dimensions: 1x0",
            "Rejecting decode with invalid dimensions: 32769x1",
            "Rejecting decode with invalid dimensions: 1x32769",
        )
  }

  @Test
  fun decodeByteArray_maximumDimension_isAccepted() {
    val expected = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    var decodeCount = 0
    val decoder = createDecoder(
        PlatformDecoderOptions(
            enableDecodeDimensionValidation = true,
            preferByteArrayDecode = true,
        ),
    )

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeByteArray(
              any<ByteArray>(),
              any<Int>(),
              any<Int>(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenAnswer { invocation ->
            decodeCount++
            val options = invocation.getArgument<BitmapFactory.Options>(3)
            if (options.inJustDecodeBounds) {
              options.outWidth = 32768
              options.outHeight = 32768
              null
            } else {
              expected
            }
          }

      val result = decoder.decode(byteArrayOf(1), 0, 1, Bitmap.Config.ARGB_8888, 1, null)

      assertThat(result).isSameAs(expected)
    }
    assertThat(decodeCount).isEqualTo(2)
  }

  @Test
  fun decodeByteArray_rejectedOptions_retriesWithoutOptionsAndReleasesBuffer() {
    val rejection = IllegalArgumentException("invalid options")
    val fallback = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val buffer = ByteBuffer.allocate(128)
    val bufferPool = RecordingBufferPool(buffer)
    val decoder = createDecoder(
        PlatformDecoderOptions(preferByteArrayDecode = true),
        bufferPool,
    )

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeByteArray(
              any<ByteArray>(),
              any<Int>(),
              any<Int>(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenThrow(rejection)
      whenever(BitmapFactory.decodeByteArray(any<ByteArray>(), any<Int>(), any<Int>()))
          .thenReturn(fallback)

      val result = decoder.decode(byteArrayOf(1), 0, 1, Bitmap.Config.ARGB_8888, 1, null)

      assertThat(result).isSameAs(fallback)
    }
    assertThat(bufferPool.released).containsExactly(buffer)
  }

  @Test
  fun decodeByteArray_failedRetry_reportsNativeDecoderError() {
    val rejection = IllegalArgumentException("invalid options")
    val reporter = RecordingErrorReporter()
    val decoder = createDecoder(
        PlatformDecoderOptions(
            catchNativeDecoderErrors = true,
            errorReporter = reporter,
            preferByteArrayDecode = true,
        ),
    )

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeByteArray(
              any<ByteArray>(),
              any<Int>(),
              any<Int>(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenThrow(rejection)
      whenever(BitmapFactory.decodeByteArray(any<ByteArray>(), any<Int>(), any<Int>()))
          .thenReturn(null)

      val result = decoder.decode(byteArrayOf(1), 0, 1, Bitmap.Config.ARGB_8888, 1, null)

      assertThat(result).isNull()
    }
    assertThat(reporter.reports)
        .containsExactly(
            ErrorReport("NATIVE_DECODER_ERROR", "Native decoder error", rejection),
        )
  }

  @Test
  fun decodeStream_withRegion_usesSampledRegionSizeForPooledBitmap() {
    val pooledBitmap = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
    val bitmapPool = RecordingBitmapPool(pooledBitmap)
    val regionDecoder = mock<BitmapRegionDecoder>()
    val options =
        BitmapFactory.Options().apply {
          outWidth = 20
          outHeight = 10
          inSampleSize = 2
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    val decoder = createDecoder(
        PlatformDecoderOptions(),
        bitmapPool = bitmapPool,
        bitmapSizeCalculator = { width, height, _ -> width * height * 4 },
    )

    mockStatic(BitmapRegionDecoder::class.java).use {
      whenever(BitmapRegionDecoder.newInstance(any<InputStream>(), any<Boolean>()))
          .thenReturn(regionDecoder)
      whenever(regionDecoder.decodeRegion(any<Rect>(), any<BitmapFactory.Options>()))
          .thenReturn(pooledBitmap)

      val result =
          decoder.decode(
              ByteArrayInputStream(byteArrayOf(1)),
              options,
              Rect(0, 0, 8, 6),
              null,
          )

      assertThat(result).isSameAs(pooledBitmap)
    }
    assertThat(bitmapPool.requestedSizes).containsExactly(48)
    assertThat(bitmapPool.released).isEmpty()
  }

  @Test
  fun decodeStream_whenDecoderReturnsDifferentBitmap_releasesBothAndThrows() {
    val pooledBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    val decodedBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    val bitmapPool = RecordingBitmapPool(pooledBitmap)
    val options = decodeOptions(width = 2, height = 2)
    val decoder = createDecoder(
        PlatformDecoderOptions(),
        bitmapPool = bitmapPool,
        bitmapSizeCalculator = { width, height, _ -> width * height * 4 },
    )

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeStream(
              any<InputStream>(),
              isNull(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenReturn(decodedBitmap)

      assertThatThrownBy {
            decoder.decode(ByteArrayInputStream(byteArrayOf(1)), options, null, null)
          }
          .isInstanceOf(IllegalStateException::class.java)
    }
    assertThat(bitmapPool.released).containsExactly(pooledBitmap)
    assertThat(decodedBitmap.isRecycled).isTrue()
  }

  @Test
  fun decodeStream_nativeErrorCaught_reportsAndReleasesResources() {
    val error = UnsatisfiedLinkError("native decoder failed")
    val reporter = RecordingErrorReporter()
    val pooledBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    val bitmapPool = RecordingBitmapPool(pooledBitmap)
    val buffer = ByteBuffer.allocate(128)
    val bufferPool = RecordingBufferPool(buffer)
    val decoder = createDecoder(
        PlatformDecoderOptions(catchNativeDecoderErrors = true, errorReporter = reporter),
        bufferPool,
        bitmapPool,
        { width, height, _ -> width * height * 4 },
    )

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeStream(
              any<InputStream>(),
              isNull(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenThrow(error)

      val result =
          decoder.decode(
              ByteArrayInputStream(byteArrayOf(1)),
              decodeOptions(width = 2, height = 2),
              null,
              null,
          )

      assertThat(result).isNull()
    }
    assertThat(reporter.reports)
        .containsExactly(
            ErrorReport("NATIVE_DECODER_ERROR", "Native decoder error", error),
        )
    assertThat(bitmapPool.released).containsExactly(pooledBitmap)
    assertThat(bufferPool.released).containsExactly(buffer)
  }

  @Test
  fun decodeJpeg_partialIncompleteInput_limitsAppendsEoiAndClosesStream() {
    val original = byteArrayOf(10, 20, 30, 40, 50, 60)
    val input = CloseTrackingInputStream(original)
    val expected = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    var decodedBytes: ByteArray? = null
    val decoder = createDecoder(PlatformDecoderOptions())

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeStream(
              any<InputStream>(),
              isNull(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenAnswer { invocation ->
            decodedBytes = invocation.getArgument<InputStream>(0).readBytes()
            expected
          }

      val result =
          decoder.decodeJpeg(
              input,
              null,
              1,
              Bitmap.Config.ARGB_8888,
              null,
              null,
              original.size,
              4,
              false,
          )

      assertThat(result).isSameAs(expected)
    }
    assertThat(decodedBytes).containsExactly(10, 20, 30, 40, -1, -39)
    assertThat(input.closed).isTrue()
  }

  @Test
  fun decode_withInvalidPrePass_throwsBeforeMainDecode() {
    val decoder = createDecoder(PlatformDecoderOptions())

    mockStatic(BitmapFactory::class.java).use {
      whenever(
          BitmapFactory.decodeStream(
              any<InputStream>(),
              isNull(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenAnswer { invocation ->
            invocation.getArgument<BitmapFactory.Options>(2).also { options ->
              options.outWidth = -1
              options.outHeight = -1
            }
            null
          }

      assertThatThrownBy {
            decoder.decode(
                ByteArrayInputStream(byteArrayOf(1)),
                ByteArrayInputStream(byteArrayOf(2)),
                1,
                Bitmap.Config.ARGB_8888,
                null,
                null,
            )
          }
          .isInstanceOf(IllegalArgumentException::class.java)
    }
  }

  private fun createDecoder(
      options: PlatformDecoderOptions,
      bufferPool: Pools.Pool<ByteBuffer> = RecordingBufferPool(ByteBuffer.allocate(128)),
      bitmapPool: BitmapPool? = null,
      bitmapSizeCalculator: ((Int, Int, BitmapFactory.Options) -> Int)? = null,
  ): DefaultRawBitmapDecoder =
      DefaultRawBitmapDecoder(bufferPool, options, bitmapPool, bitmapSizeCalculator)

  private fun decodeOptions(width: Int, height: Int): BitmapFactory.Options =
      BitmapFactory.Options().apply {
        outWidth = width
        outHeight = height
        inSampleSize = 1
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

  private fun encodePng(width: Int, height: Int): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val output = ByteArrayOutputStream()
    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
    bitmap.recycle()
    return output.toByteArray()
  }

  private class RecordingBufferPool(private val buffer: ByteBuffer?) : Pools.Pool<ByteBuffer> {
    val released = mutableListOf<ByteBuffer>()

    override fun acquire(): ByteBuffer? = buffer

    override fun release(instance: ByteBuffer): Boolean {
      released.add(instance)
      return true
    }
  }

  private class RecordingBitmapPool(private val bitmap: Bitmap) : BitmapPool {
    val requestedSizes = mutableListOf<Int>()
    val released = mutableListOf<Bitmap>()

    override fun get(size: Int): Bitmap {
      requestedSizes.add(size)
      return bitmap
    }

    override fun release(value: Bitmap) {
      released.add(value)
    }

    override fun trim(trimType: MemoryTrimType) = Unit
  }

  private class RecordingErrorReporter : PlatformDecoderOptions.DecoderErrorReporter {
    val reports = mutableListOf<ErrorReport>()

    override fun reportError(category: String, message: String, cause: Throwable?) {
      reports.add(ErrorReport(category, message, cause))
    }
  }

  private data class ErrorReport(
      val category: String,
      val message: String,
      val cause: Throwable?,
  )

  private class CloseTrackingInputStream(data: ByteArray) : ByteArrayInputStream(data) {
    var closed = false

    override fun close() {
      closed = true
      super.close()
    }
  }
}
