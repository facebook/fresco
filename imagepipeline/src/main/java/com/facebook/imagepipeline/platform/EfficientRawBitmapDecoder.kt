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
    // Input validation
    if (!DecodeValidationUtils.validateInput(data, offset, length)) return null
    if (!DecodeValidationUtils.isValidImageHeader(data, offset, length)) return null

    val retryOnFail = bitmapConfig != Bitmap.Config.ARGB_8888
    return try {
      decodeInternal(data, offset, length, bitmapConfig, sampleSize, colorSpace)
    } catch (re: RuntimeException) {
      if (retryOnFail) {
        decodeInternal(data, offset, length, Bitmap.Config.ARGB_8888, sampleSize, colorSpace)
      } else {
        throw re
      }
    }
  }

  private fun decodeInternal(
      data: ByteArray,
      offset: Int,
      length: Int,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      // On API 28-32 with a non-null colorSpace, fall through to BitmapFactory path
      // because ImageDecoder doesn't support setTargetColorSpace until API 33.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || colorSpace == null) {
        return try {
          ImageDecoder.decodeBitmap(
              ImageDecoder.createSource(ByteBuffer.wrap(data, offset, length))
          ) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSampleSize(sampleSize)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && colorSpace != null) {
              decoder.setTargetColorSpace(colorSpace)
            }
          }
        } catch (e: IOException) {
          null
        }
      }
    }
    // BitmapFactory path for API < 28, or API 28-32 with non-null colorSpace
    val options = BitmapFactory.Options()
    options.inSampleSize = sampleSize
    options.inPreferredConfig = bitmapConfig
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB)
    }
    var byteBuffer = decodeBuffers.acquire()
    if (byteBuffer == null) {
      byteBuffer = ByteBuffer.allocate(DecodeBufferHelper.getRecommendedDecodeBufferSize())
    }
    try {
      options.inTempStorage = byteBuffer.array()
      return BitmapFactory.decodeByteArray(data, offset, length, options)
    } catch (error: Error) {
      if (platformDecoderOptions.catchNativeDecoderErrors) {
        FLog.e(TAG, "Native decoder error", error)
        platformDecoderOptions.errorReporter?.reportError(
            "NATIVE_DECODER_ERROR",
            "Native decoder error",
            error,
        )
        return null
      }
      throw error
    } finally {
      decodeBuffers.release(byteBuffer)
    }
  }

  companion object {
    private const val TAG = "EfficientRawBitmapDecoder"
  }
}
