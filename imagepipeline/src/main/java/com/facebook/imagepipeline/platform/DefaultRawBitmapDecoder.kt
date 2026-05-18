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
import com.facebook.common.logging.FLog
import com.facebook.common.memory.DecodeBufferHelper
import com.facebook.common.streams.LimitedInputStream
import com.facebook.common.streams.TailAppendingInputStream
import com.facebook.imagepipeline.memory.BitmapPool
import com.facebook.imageutils.JfifUtil
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Locale
import javax.annotation.concurrent.ThreadSafe

/**
 * Pool-free bitmap decoder using [BitmapFactory.decodeStream]. Produces mutable bitmaps. Delegates
 * to [RawBitmapDecoder] interface for use by both DefaultDecoder and IG adapter.
 */
@ThreadSafe
class DefaultRawBitmapDecoder(
    private val decodeBuffers: Pools.Pool<ByteBuffer>,
    private val platformDecoderOptions: PlatformDecoderOptions,
    private val bitmapPool: BitmapPool? = null,
    private val bitmapSizeCalculator: ((Int, Int, BitmapFactory.Options) -> Int)? = null,
) : RawBitmapDecoder {

  private val preverificationHelper: PreverificationHelper? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) PreverificationHelper() else null

  override fun decode(
      data: ByteArray,
      offset: Int,
      length: Int,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    return decode(ByteArrayInputStream(data, offset, length), bitmapConfig, sampleSize, colorSpace)
  }

  override fun decode(
      stream: InputStream,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    val options = buildDecodeOptions(sampleSize, null, bitmapConfig)

    return try {
      decode(stream, options, null, colorSpace)
    } catch (error: Error) {
      if (platformDecoderOptions.catchNativeDecoderErrors) {
        FLog.e(TAG, "Native decoder error", error)
        platformDecoderOptions.errorReporter?.reportError(
            "NATIVE_DECODER_ERROR",
            "Native decoder error",
            error,
        )
        null
      } else {
        throw error
      }
    }
  }

  /**
   * Full decode-from-stream with bitmap pool management, region decoding, hardware bitmap support,
   * and error handling. Ported from DefaultDecoder.decodeFromStream().
   *
   * @return the decoded bitmap, or null if decoding was rejected/failed
   */
  override fun decode(
      decodeStream: InputStream,
      prePassStream: InputStream?,
      sampleSize: Int,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    val options = buildDecodeOptions(sampleSize, prePassStream, bitmapConfig)
    return decode(decodeStream, options, regionToDecode, colorSpace)
  }

  /**
   * Full decode-from-stream with pre-built options. Handles bitmap pool management, region
   * decoding, hardware bitmap support, and error handling.
   */
  override fun decode(
      inputStream: InputStream,
      options: BitmapFactory.Options,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    var targetWidth = options.outWidth
    var targetHeight = options.outHeight

    // Reject images with invalid or excessively large dimensions before passing to the native
    // decoder. Some vendor JPEG libraries (libjpeg-alpha.so) crash on malformed or oversized data.
    if (
        platformDecoderOptions.enableDecodeDimensionValidation &&
            (targetWidth <= 0 ||
                targetHeight <= 0 ||
                targetWidth > MAX_DECODE_DIMENSION ||
                targetHeight > MAX_DECODE_DIMENSION)
    ) {
      val message =
          String.format(
              Locale.ROOT,
              "Rejecting decode with invalid dimensions: %dx%d",
              targetWidth,
              targetHeight,
          )
      FLog.e(TAG, message)
      platformDecoderOptions.errorReporter?.reportError(
          "DECODE_DIMENSION_VALIDATION",
          message,
          null,
      )
      return null
    }

    if (regionToDecode != null) {
      targetWidth = regionToDecode.width() / options.inSampleSize
      targetHeight = regionToDecode.height() / options.inSampleSize
    }

    var bitmapToReuse: Bitmap? = null
    var shouldUseHardwareBitmapConfig = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      shouldUseHardwareBitmapConfig =
          preverificationHelper?.shouldUseHardwareBitmapConfig(options.inPreferredConfig) == true
    }

    if (regionToDecode == null && shouldUseHardwareBitmapConfig) {
      // Cannot reuse bitmaps with Bitmap.Config.HARDWARE
      options.inMutable = false
    } else {
      if (regionToDecode != null && shouldUseHardwareBitmapConfig) {
        // If region decoding was requested we need to fallback to default config
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      if (
          !platformDecoderOptions.avoidPoolGet && bitmapPool != null && bitmapSizeCalculator != null
      ) {
        val sizeInBytes = bitmapSizeCalculator.invoke(targetWidth, targetHeight, options)
        bitmapToReuse =
            bitmapPool.get(sizeInBytes)
                ?: throw NullPointerException("BitmapPool.get returned null")
      }
    }

    // inBitmap can be nullable
    options.inBitmap = bitmapToReuse

    // Performs transformation at load time to target color space.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB)
    }

    var decodedBitmap: Bitmap? = null
    try {
      if (regionToDecode != null && bitmapToReuse != null && options.inPreferredConfig != null) {
        var byteBuffer = decodeBuffers.acquire()
        if (byteBuffer == null) {
          byteBuffer = ByteBuffer.allocate(DecodeBufferHelper.getRecommendedDecodeBufferSize())
        }
        try {
          options.inTempStorage = byteBuffer.array()
          var bitmapRegionDecoder: BitmapRegionDecoder? = null
          try {
            bitmapToReuse.reconfigure(targetWidth, targetHeight, options.inPreferredConfig)
            bitmapRegionDecoder = BitmapRegionDecoder.newInstance(inputStream, true)
            if (bitmapRegionDecoder != null) {
              decodedBitmap = bitmapRegionDecoder.decodeRegion(regionToDecode, options)
            }
          } catch (e: IOException) {
            FLog.e(TAG, "Could not decode region %s, decoding full bitmap instead.", regionToDecode)
          } finally {
            bitmapRegionDecoder?.recycle()
          }
        } finally {
          decodeBuffers.release(byteBuffer)
        }
      }
      if (decodedBitmap == null) {
        var byteBuffer = decodeBuffers.acquire()
        if (byteBuffer == null) {
          byteBuffer = ByteBuffer.allocate(DecodeBufferHelper.getRecommendedDecodeBufferSize())
        }
        try {
          options.inTempStorage = byteBuffer.array()
          decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options)
        } finally {
          decodeBuffers.release(byteBuffer)
        }
      }
    } catch (e: IllegalArgumentException) {
      if (bitmapToReuse != null) {
        bitmapPool?.release(bitmapToReuse)
      }
      // This is thrown if the Bitmap options are invalid, so let's just try to decode the bitmap
      // as-is, which might be inefficient - but it works.
      try {
        // We need to reset the stream first
        inputStream.reset()

        val naiveDecodedBitmap = BitmapFactory.decodeStream(inputStream)
        if (naiveDecodedBitmap == null) {
          throw e
        }
        return naiveDecodedBitmap
      } catch (re: IOException) {
        // We throw the original exception instead since it's the one causing this workaround in
        // the first place.
        throw e
      }
    } catch (re: RuntimeException) {
      if (bitmapToReuse != null) {
        bitmapPool?.release(bitmapToReuse)
      }
      throw re
    } catch (error: Error) {
      // Catch native decoder errors (e.g. from buggy vendor JPEG libraries like libjpeg-alpha.so)
      // that surface as UnsatisfiedLinkError or other Errors. Release the pooled bitmap and
      // return null so the caller treats this as a failed decode instead of crashing.
      if (bitmapToReuse != null) {
        bitmapPool?.release(bitmapToReuse)
      }
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
    }

    // If bitmap with Bitmap.Config.HARDWARE was used, `bitmapToReuse` will be null and it's
    // expected
    if (bitmapToReuse != null && bitmapToReuse !== decodedBitmap) {
      bitmapPool?.release(bitmapToReuse)
      decodedBitmap?.recycle()
      throw IllegalStateException()
    }

    return decodedBitmap
  }

  override fun decodeJpeg(
      jpegStream: InputStream,
      prePassStream: InputStream?,
      sampleSize: Int,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
      encodedImageSize: Int,
      length: Int,
      isJpegComplete: Boolean,
  ): Bitmap? {
    val options = buildDecodeOptions(sampleSize, prePassStream, bitmapConfig)
    var stream = jpegStream
    if (encodedImageSize > length) {
      stream = LimitedInputStream(stream, length)
    }
    if (!isJpegComplete) {
      stream = TailAppendingInputStream(stream, EOI_TAIL)
    }
    return try {
      decode(stream, options, regionToDecode, colorSpace)
    } finally {
      try {
        stream.close()
      } catch (e: IOException) {
        e.printStackTrace()
      }
    }
  }

  private fun buildDecodeOptions(
      sampleSize: Int,
      prePassStream: InputStream?,
      bitmapConfig: Bitmap.Config,
  ): BitmapFactory.Options {
    val options = BitmapFactory.Options()
    options.inSampleSize = sampleSize
    options.inJustDecodeBounds = true
    options.inDither = true
    val isHardwareBitmap =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmapConfig == Bitmap.Config.HARDWARE
    if (!isHardwareBitmap) {
      options.inPreferredConfig = bitmapConfig
    }
    options.inMutable = true
    if (prePassStream != null) {
      BitmapFactory.decodeStream(prePassStream, null, options)
      if (options.outWidth == -1 || options.outHeight == -1) {
        throw IllegalArgumentException()
      }
    }
    if (isHardwareBitmap) {
      options.inPreferredConfig = bitmapConfig
    }
    options.inJustDecodeBounds = false
    return options
  }

  companion object {
    private const val TAG = "DefaultRawBitmapDecoder"
    private const val MAX_DECODE_DIMENSION = 32768
    private val EOI_TAIL =
        byteArrayOf(JfifUtil.MARKER_FIRST_BYTE.toByte(), JfifUtil.MARKER_EOI.toByte())
  }
}
