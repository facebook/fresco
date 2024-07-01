/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Pair
import androidx.core.util.Pools
import com.facebook.common.memory.DecodeBufferHelper
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

/** This class contains utility method for Bitmap */
object BitmapUtil {

  private const val POOL_SIZE = 12
  private val DECODE_BUFFERS: Pools.SynchronizedPool<ByteBuffer> by lazy {
    Pools.SynchronizedPool(POOL_SIZE)
  }

  /** Bytes per pixel definitions */
  const val ALPHA_8_BYTES_PER_PIXEL: Int = 1
  const val ARGB_4444_BYTES_PER_PIXEL: Int = 2
  const val ARGB_8888_BYTES_PER_PIXEL: Int = 4
  const val RGB_565_BYTES_PER_PIXEL: Int = 2
  const val RGBA_F16_BYTES_PER_PIXEL: Int = 8
  const val RGBA_1010102_BYTES_PER_PIXEL = 4
  const val MAX_BITMAP_DIMENSION: Float = 2_048f
  private var useDecodeBufferHelper = false
  private var fixDecodeDrmImageCrash = false

  /** @return size in bytes of the underlying bitmap */
  @JvmStatic
  @SuppressLint("NewApi")
  fun getSizeInBytes(bitmap: Bitmap?): Int {
    if (bitmap == null) {
      return 0
    }

    // There's a known issue in KitKat where getAllocationByteCount() can throw an NPE. This was
    // apparently fixed in MR1: http://bit.ly/1IvdRpd. So we do a version check here, and
    // catch any potential NPEs just to be safe.
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
      try {
        return bitmap.allocationByteCount
      } catch (npe: NullPointerException) {
        // Swallow exception and try fallbacks.
      }
    }
    return bitmap.byteCount
  }

  /**
   * Decodes only the bounds of an image and returns its width and height or null if the size can't
   * be determined
   *
   * @param bytes the input byte array of the image
   * @return dimensions of the image
   */
  @JvmStatic
  fun decodeDimensions(bytes: ByteArray?): Pair<Int, Int>? =
      // wrapping with ByteArrayInputStream is cheap and we don't have duplicate implementation
      decodeDimensions(ByteArrayInputStream(bytes))

  /**
   * Decodes the bounds of an image from its Uri and returns a pair of the dimensions
   *
   * @param uri the Uri of the image
   * @return dimensions of the image
   */
  @JvmStatic
  fun decodeDimensions(uri: Uri): Pair<Int, Int>? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(uri.path, options)
    return if (options.outWidth == -1 || options.outHeight == -1) null
    else Pair(options.outWidth, options.outHeight)
  }

  /**
   * Decodes the bounds of an image and returns its width and height or null if the size can't be
   * determined
   *
   * @param inputStream the InputStream containing the image data
   * @return dimensions of the image
   */
  @JvmStatic
  fun decodeDimensions(inputStream: InputStream?): Pair<Int, Int>? {
    checkNotNull(inputStream)
    val byteBuffer = obtainByteBuffer()
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    return try {
      options.inTempStorage = byteBuffer.array()
      decodeStreamInternal(inputStream, null, options)
      if (options.outWidth == -1 || options.outHeight == -1) null
      else Pair(options.outWidth, options.outHeight)
    } finally {
      releaseByteBuffer(byteBuffer)
    }
  }

  /**
   * Decodes the bounds of an image and returns its width and height or null if the size can't be
   * determined. It also recovers the color space of the image, or null if it can't be determined.
   *
   * @param inputStream the InputStream containing the image data
   * @return the metadata of the image
   */
  @JvmStatic
  fun decodeDimensionsAndColorSpace(inputStream: InputStream?): ImageMetaData {
    checkNotNull(inputStream)
    val byteBuffer = obtainByteBuffer()
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    return try {
      options.inTempStorage = byteBuffer.array()
      decodeStreamInternal(inputStream, null, options)
      val colorSpace: ColorSpace? =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) options.outColorSpace else null

      ImageMetaData(options.outWidth, options.outHeight, colorSpace)
    } finally {
      releaseByteBuffer(byteBuffer)
    }
  }

  fun decodeStreamInternal(
      inputStream: InputStream?,
      outPadding: Rect?,
      options: BitmapFactory.Options?
  ): Bitmap? {
    if (this.fixDecodeDrmImageCrash) {
      return try {
        BitmapFactory.decodeStream(inputStream, outPadding, options)
      } catch (ex: IllegalArgumentException) {
        null
      }
    }
    return BitmapFactory.decodeStream(inputStream, outPadding, options)
  }

  /**
   * Returns the amount of bytes used by a pixel in a specific [ ]
   *
   * @param bitmapConfig the [android.graphics.Bitmap.Config] for which the size in byte will be
   *   returned
   * @return
   */
  @SuppressLint("NewApi")
  @JvmStatic
  fun getPixelSizeForBitmapConfig(bitmapConfig: Bitmap.Config?): Int =
      when (bitmapConfig) {
        Bitmap.Config.ARGB_8888 -> ARGB_8888_BYTES_PER_PIXEL
        Bitmap.Config.ALPHA_8 -> ALPHA_8_BYTES_PER_PIXEL
        Bitmap.Config.ARGB_4444 -> ARGB_4444_BYTES_PER_PIXEL
        Bitmap.Config.RGB_565 -> RGB_565_BYTES_PER_PIXEL
        Bitmap.Config.RGBA_F16 -> RGBA_F16_BYTES_PER_PIXEL
        Bitmap.Config.RGBA_1010102 -> RGBA_1010102_BYTES_PER_PIXEL
        Bitmap.Config.HARDWARE ->
            ARGB_8888_BYTES_PER_PIXEL // We assume ARGB_8888 is used underneath
        else -> throw UnsupportedOperationException("The provided Bitmap.Config is not supported")
      }

  /**
   * Returns the size in byte of an image with specific size and [ ]
   *
   * @param width the width of the image
   * @param height the height of the image
   * @param bitmapConfig the [android.graphics.Bitmap.Config] for which the size in byte will be
   *   returned
   * @return calculated size in bytes
   * @throws IllegalArgumentException if width or height is less than or equal to zero
   * @throws IllegalStateException if calculated size overflows Integer datatype
   */
  @JvmStatic
  fun getSizeInByteForBitmap(width: Int, height: Int, bitmapConfig: Bitmap.Config?): Int {
    require(width > 0) { "width must be > 0, width is: $width" }
    require(height > 0) { "height must be > 0, height is: $height" }
    val pixelSize = getPixelSizeForBitmapConfig(bitmapConfig)
    val size = width * height * pixelSize
    check(size > 0) {
      "size must be > 0: size: $size, width: $width, height: $height, pixelSize: $pixelSize"
    }
    return size
  }

  private fun acquireByteBuffer(): ByteBuffer? =
      if (useDecodeBufferHelper) {
        DecodeBufferHelper.INSTANCE.acquire()
      } else {
        DECODE_BUFFERS.acquire()
      }

  private fun releaseByteBuffer(byteBuffer: ByteBuffer) {
    if (!useDecodeBufferHelper) {
      DECODE_BUFFERS.release(byteBuffer)
    }
  }

  @JvmStatic
  fun setUseDecodeBufferHelper(useDecodeBufferHelper: Boolean) {
    this.useDecodeBufferHelper = useDecodeBufferHelper
  }

  @JvmStatic
  fun setFixDecodeDrmImageCrash(fixDecodeDrmImageCrash: Boolean) {
    this.fixDecodeDrmImageCrash = fixDecodeDrmImageCrash
  }

  private fun obtainByteBuffer(): ByteBuffer {
    return acquireByteBuffer()
        ?: ByteBuffer.allocate(DecodeBufferHelper.getRecommendedDecodeBufferSize())
  }
}
