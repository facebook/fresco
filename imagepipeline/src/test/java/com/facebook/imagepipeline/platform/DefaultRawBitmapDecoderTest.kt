/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.util.Pools
import java.nio.ByteBuffer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests the byte-array decode failure handling in [DefaultRawBitmapDecoder], covering both the
 * naive [BitmapFactory] retry and the reporting performed when
 * [PlatformDecoderOptions.disableDefaultDecoderRetry] is set.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class DefaultRawBitmapDecoderTest {

  private lateinit var errorReporter: RecordingErrorReporter
  private lateinit var encodedBytes: ByteArray

  @Before
  fun setUp() {
    errorReporter = RecordingErrorReporter()
    encodedBytes = ByteArray(ENCODED_BYTES_LENGTH)
  }

  @Test
  fun retryEnabled_whenNaiveRetryReturnsNull_reportsNativeDecoderError() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      stubDecodeWithOptionsToThrow()
      whenever(BitmapFactory.decodeByteArray(any<ByteArray>(), any<Int>(), any<Int>()))
          .thenReturn(null)

      val result = decode(createDecoder(disableDefaultDecoderRetry = false))

      assertThat(result).isNull()
      assertThat(errorReporter.categories).containsExactly("NATIVE_DECODER_ERROR")
      assertThat(errorReporter.causes.single()).isInstanceOf(IllegalArgumentException::class.java)
      verifyNaiveRetryCount(mockedBitmapFactory, times(1))
    }
  }

  @Test
  fun retryEnabled_whenNaiveRetrySucceeds_returnsBitmapAndReportsNothing() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      val fallbackBitmap = mock<Bitmap>()
      stubDecodeWithOptionsToThrow()
      whenever(BitmapFactory.decodeByteArray(any<ByteArray>(), any<Int>(), any<Int>()))
          .thenReturn(fallbackBitmap)

      val result = decode(createDecoder(disableDefaultDecoderRetry = false))

      assertThat(result).isSameAs(fallbackBitmap)
      assertThat(errorReporter.categories).isEmpty()
      verifyNaiveRetryCount(mockedBitmapFactory, times(1))
    }
  }

  @Test
  fun retryDisabled_reportsDecodeIaeAndSkipsNaiveRetry() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      stubDecodeWithOptionsToThrow()

      val result = decode(createDecoder(disableDefaultDecoderRetry = true))

      assertThat(result).isNull()
      assertThat(errorReporter.categories).containsExactly("DECODE_IAE")
      assertThat(errorReporter.causes.single()).isInstanceOf(IllegalArgumentException::class.java)
      verifyNaiveRetryCount(mockedBitmapFactory, never())
    }
  }

  @Test
  fun retryDisabled_whenDecodeThrowsError_reportsDecodeError() {
    Mockito.mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
      whenever(
          BitmapFactory.decodeByteArray(
              any<ByteArray>(),
              any<Int>(),
              any<Int>(),
              any<BitmapFactory.Options>(),
          ),
      )
          .thenThrow(UnsatisfiedLinkError("libjpeg-alpha.so"))

      val result = decode(createDecoder(disableDefaultDecoderRetry = true))

      assertThat(result).isNull()
      assertThat(errorReporter.categories).containsExactly("DECODE_ERROR")
      verifyNaiveRetryCount(mockedBitmapFactory, never())
    }
  }

  /** Pool-free decoder so [DefaultRawBitmapDecoder.decode] takes the byte-array fast path. */
  private fun createDecoder(disableDefaultDecoderRetry: Boolean): DefaultRawBitmapDecoder {
    val decodeBuffers = Pools.SynchronizedPool<ByteBuffer>(1)
    decodeBuffers.release(ByteBuffer.allocate(DECODE_BUFFER_SIZE))
    return DefaultRawBitmapDecoder(
        decodeBuffers,
        PlatformDecoderOptions(
            catchNativeDecoderErrors = true,
            errorReporter = errorReporter,
            preferByteArrayDecode = true,
            disableDefaultDecoderRetry = disableDefaultDecoderRetry,
        ),
    )
  }

  private fun decode(decoder: DefaultRawBitmapDecoder): Bitmap? =
      decoder.decode(encodedBytes, 0, encodedBytes.size, DEFAULT_BITMAP_CONFIG, 1, null)

  private fun stubDecodeWithOptionsToThrow() {
    whenever(
        BitmapFactory.decodeByteArray(
            any<ByteArray>(),
            any<Int>(),
            any<Int>(),
            any<BitmapFactory.Options>(),
        ),
    )
        .thenThrow(IllegalArgumentException("bad decode options"))
  }

  private fun verifyNaiveRetryCount(
      mockedBitmapFactory: org.mockito.MockedStatic<BitmapFactory>,
      mode: org.mockito.verification.VerificationMode,
  ) {
    mockedBitmapFactory.verify(
        { BitmapFactory.decodeByteArray(any<ByteArray>(), any<Int>(), any<Int>()) },
        mode,
    )
  }

  private class RecordingErrorReporter : PlatformDecoderOptions.DecoderErrorReporter {
    val categories = mutableListOf<String>()
    val causes = mutableListOf<Throwable?>()

    override fun reportError(category: String, message: String, cause: Throwable?) {
      categories.add(category)
      causes.add(cause)
    }
  }

  companion object {
    private val DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private const val ENCODED_BYTES_LENGTH = 128
    private const val DECODE_BUFFER_SIZE = 16 * 1024
  }
}
