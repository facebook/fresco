/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.os.Build
import androidx.core.util.Pools
import com.facebook.common.logging.FLog
import com.facebook.common.memory.DecodeBufferHelper
import java.io.IOException
import java.nio.ByteBuffer
import javax.annotation.concurrent.ThreadSafe

/**
 * Decode engine that implements [RawBitmapDecoder] for direct byte-array-to-Bitmap decoding.
 *
 * Decode logic adapted from DefaultDecoder.decodeFromStream().
 *
 * On API 28+ uses [ImageDecoder]; on API 33+ also supports [ColorSpace] via
 * [ImageDecoder.setTargetColorSpace]. On API 26-27 (or API 28-32 with a non-null [ColorSpace]),
 * falls back to [BitmapFactory] with [BitmapFactory.Options.inPreferredColorSpace].
 */
@ThreadSafe
class EfficientRawBitmapDecoder(
    private val decodeBuffers: Pools.Pool<ByteBuffer>,
    private val platformDecoderOptions: PlatformDecoderOptions,
) : RawBitmapDecoder {

  override fun decode(
      data: ByteArray,
      offset: Int,
      length: Int,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    if (platformDecoderOptions.enableInputValidation) {
      if (!DecodeValidationUtils.validateInput(data, offset, length)) return null
      if (!DecodeValidationUtils.isValidImageHeader(data, offset, length)) return null
    }
    return decodeInternal(data, offset, length, bitmapConfig, sampleSize, colorSpace)
  }

  private fun decodeInternal(
      data: ByteArray,
      offset: Int,
      length: Int,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    if (
        !platformDecoderOptions.useBitmapFactoryDecoder &&
            bitmapConfig != Bitmap.Config.RGB_565 &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || colorSpace == null) {
        return decodeWithImageDecoder(data, offset, length, sampleSize, colorSpace)
      }
    }
    return decodeWithBitmapFactory(data, offset, length, bitmapConfig, sampleSize, colorSpace)
  }

  @android.annotation.SuppressLint("NewApi")
  private fun decodeWithImageDecoder(
      data: ByteArray,
      offset: Int,
      length: Int,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    return try {
      ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(data, offset, length))) {
          decoder,
          _,
          _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = !platformDecoderOptions.decodeImmutableBitmaps
        decoder.setTargetSampleSize(sampleSize)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && colorSpace != null) {
          decoder.setTargetColorSpace(colorSpace)
        }
      }
    } catch (e: IOException) {
      if (platformDecoderOptions.enableDecodeRetry)
          naiveRetryOrNull(data, offset, length, e, sampleSize = sampleSize)
      else null
    } catch (e: IllegalArgumentException) {
      if (platformDecoderOptions.enableDecodeRetry)
          naiveRetryOrNull(data, offset, length, e, sampleSize = sampleSize)
      else reportOrNull(e)
    } catch (e: UnsatisfiedLinkError) {
      if (platformDecoderOptions.enableDecodeRetry)
          naiveRetryOrNull(data, offset, length, e, sampleSize = sampleSize)
      else reportOrNull(e)
    } catch (e: OutOfMemoryError) {
      if (platformDecoderOptions.enableDecodeRetry)
          lowFidelityRetryOrNull(data, offset, length, e, sampleSize)
      else reportOrNull(e)
    } catch (re: RuntimeException) {
      reportOrNull(re)
    } catch (error: Error) {
      reportOrNull(error)
    }
  }

  private fun naiveRetryOrNull(
      data: ByteArray,
      offset: Int,
      length: Int,
      cause: Throwable,
      bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
      sampleSize: Int = 1,
  ): Bitmap? {
    FLog.e(TAG, "Decode failed, retrying with BitmapFactory", cause)
    val retryOptions = BitmapFactory.Options()
    retryOptions.inPreferredConfig = bitmapConfig
    if (sampleSize > 1) {
      retryOptions.inSampleSize = sampleSize
    }
    return try {
      BitmapFactory.decodeByteArray(data, offset, length, retryOptions)
          ?: run {
            platformDecoderOptions.errorReporter?.reportError(
                "NATIVE_DECODER_ERROR",
                "Decode failed after retry",
                cause,
            )
            null
          }
    } catch (t: Throwable) {
      reportOrNull(t)
    }
  }

  private fun lowFidelityRetryOrNull(
      data: ByteArray,
      offset: Int,
      length: Int,
      cause: Throwable,
      sampleSize: Int = 1,
  ): Bitmap? {
    val retryOptions = BitmapFactory.Options()
    if (platformDecoderOptions.respectLowFidelityConfig) {
      FLog.e(TAG, "OOM during decode, retrying with RGB_565", cause)
      retryOptions.inPreferredConfig = Bitmap.Config.RGB_565
      retryOptions.inSampleSize = sampleSize
    } else {
      FLog.e(TAG, "OOM during decode, retrying with reduced resolution", cause)
      retryOptions.inSampleSize = maxOf(sampleSize * 2, 2)
    }
    return try {
      BitmapFactory.decodeByteArray(data, offset, length, retryOptions)
          ?: run {
            platformDecoderOptions.errorReporter?.reportError(
                "NATIVE_DECODER_ERROR",
                "OOM retry also failed",
                cause,
            )
            null
          }
    } catch (t: Throwable) {
      reportOrNull(t)
    }
  }

  private fun reportOrNull(cause: Throwable): Bitmap? {
    if (platformDecoderOptions.catchNativeDecoderErrors) {
      FLog.e(TAG, "Native decoder error", cause)
      platformDecoderOptions.errorReporter?.reportError(
          "NATIVE_DECODER_ERROR",
          "Native decoder error",
          cause,
      )
      return null
    }
    throw cause
  }

  private fun decodeWithBitmapFactory(
      data: ByteArray,
      offset: Int,
      length: Int,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    val options = BitmapFactory.Options()
    options.inSampleSize = sampleSize
    options.inPreferredConfig = bitmapConfig
    if (!platformDecoderOptions.decodeImmutableBitmaps) {
      options.inMutable = true
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB)
    }
    var byteBuffer = decodeBuffers.acquire()
    if (byteBuffer == null) {
      byteBuffer = ByteBuffer.allocate(DecodeBufferHelper.getRecommendedDecodeBufferSize())
    }
    return try {
      options.inTempStorage = byteBuffer.array()
      BitmapFactory.decodeByteArray(data, offset, length, options)
    } catch (e: IllegalArgumentException) {
      if (platformDecoderOptions.enableDecodeRetry)
          naiveRetryOrNull(data, offset, length, e, bitmapConfig, sampleSize)
      else reportOrNull(e)
    } catch (e: OutOfMemoryError) {
      if (platformDecoderOptions.enableDecodeRetry)
          lowFidelityRetryOrNull(data, offset, length, e, sampleSize)
      else reportOrNull(e)
    } catch (e: UnsatisfiedLinkError) {
      if (platformDecoderOptions.enableDecodeRetry)
          naiveRetryOrNull(data, offset, length, e, bitmapConfig, sampleSize)
      else reportOrNull(e)
    } catch (re: RuntimeException) {
      reportOrNull(re)
    } catch (error: Error) {
      reportOrNull(error)
    } finally {
      decodeBuffers.release(byteBuffer)
    }
  }

  companion object {
    private const val TAG = "EfficientRawBitmapDecoder"
  }
}
