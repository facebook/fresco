/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Rect
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.DoNotStrip
import com.facebook.common.internal.Throwables
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.common.TooManyBitmapsException
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.memory.BitmapCounterProvider
import com.facebook.imagepipeline.platform.PlatformDecoder
import com.facebook.imageutils.BitmapUtil
import com.facebook.imageutils.JfifUtil
import com.facebook.soloader.DoNotOptimize
import java.util.Locale

/**
 * Base class for bitmap decodes for Dalvik VM (Gingerbread to KitKat).
 *
 * Native code used by this class is shipped as part of libimagepipeline.so
 */
@DoNotStrip
abstract class DalvikPurgeableDecoder protected constructor() : PlatformDecoder {

  private val unpooledBitmapsCounter = BitmapCounterProvider.get()

  override fun decodeFromEncodedImage(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
  ): CloseableReference<Bitmap>? =
      decodeFromEncodedImageWithColorSpace(encodedImage, bitmapConfig, regionToDecode, null)

  override fun decodeJPEGFromEncodedImage(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      length: Int,
  ): CloseableReference<Bitmap>? =
      decodeJPEGFromEncodedImageWithColorSpace(
          encodedImage,
          bitmapConfig,
          regionToDecode,
          length,
          null,
      )

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param bitmapConfig the [android.graphics.Bitmap.Config] used to create the decoded Bitmap
   * @param regionToDecode optional image region to decode. currently not supported.
   * @param colorSpace the target color space of the decoded bitmap, must be one of the named color
   *   space in [android.graphics.ColorSpace.Named]. If null, then SRGB color space is assumed if
   *   the SDK version >= 26.
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  override fun decodeFromEncodedImageWithColorSpace(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
  ): CloseableReference<Bitmap>? {
    val options = getBitmapFactoryOptions(encodedImage.sampleSize, bitmapConfig)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      OreoUtils.setColorSpace(options, colorSpace)
    }
    val bytesRef = encodedImage.byteBufferRef
    checkNotNull(bytesRef)
    try {
      val bitmap = decodeByteArrayAsPurgeable(bytesRef, options)
      return pinBitmap(bitmap)
    } finally {
      CloseableReference.closeSafely(bytesRef)
    }
  }

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param bitmapConfig the [android.graphics.Bitmap.Config] used to create the decoded Bitmap
   * @param regionToDecode optional image region to decode. currently not supported.
   * @param length the number of encoded bytes in the buffer
   * @param colorSpace the target color space of the decoded bitmap, must be one of the named color
   *   space in [android.graphics.ColorSpace.Named]. If null, then SRGB color space is assumed if
   *   the SDK version >= 26.
   * @return the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  override fun decodeJPEGFromEncodedImageWithColorSpace(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      length: Int,
      colorSpace: ColorSpace?,
  ): CloseableReference<Bitmap>? {
    val options = getBitmapFactoryOptions(encodedImage.sampleSize, bitmapConfig)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      OreoUtils.setColorSpace(options, colorSpace)
    }
    val bytesRef = encodedImage.byteBufferRef
    checkNotNull(bytesRef)
    try {
      val bitmap = decodeJPEGByteArrayAsPurgeable(bytesRef, length, options)
      return pinBitmap(bitmap)
    } finally {
      CloseableReference.closeSafely(bytesRef)
    }
  }

  /**
   * Decodes a byteArray into a purgeable bitmap
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param options the options passed to the BitmapFactory
   * @return
   */
  protected abstract fun decodeByteArrayAsPurgeable(
      bytesRef: CloseableReference<PooledByteBuffer>,
      options: BitmapFactory.Options,
  ): Bitmap

  /**
   * Decodes a byteArray containing jpeg encoded bytes into a purgeable bitmap
   *
   * Adds a JFIF End-Of-Image marker if needed before decoding.
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param length the number of encoded bytes in the buffer
   * @param options the options passed to the BitmapFactory
   * @return
   */
  protected abstract fun decodeJPEGByteArrayAsPurgeable(
      bytesRef: CloseableReference<PooledByteBuffer>,
      length: Int,
      options: BitmapFactory.Options,
  ): Bitmap

  @DoNotOptimize
  private object OreoUtils {
    @JvmStatic
    @TargetApi(Build.VERSION_CODES.O)
    fun setColorSpace(options: BitmapFactory.Options, colorSpace: ColorSpace?) {
      options.inPreferredColorSpace = colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB)
    }
  }

  /**
   * Pin the bitmap so that it cannot be 'purged'. Only makes sense for purgeable bitmaps WARNING:
   * Use with caution. Make sure that the pinned bitmap is recycled eventually. Otherwise, this will
   * simply eat up ashmem memory and eventually lead to unfortunate crashes. We *may* eventually
   * provide an unpin method - but we don't yet have a compelling use case for that.
   *
   * @param bitmap the purgeable bitmap to pin
   */
  fun pinBitmap(bitmap: Bitmap): CloseableReference<Bitmap> {
    checkNotNull(bitmap)
    try {
      // Real decoding happens here - if the image was corrupted, this will throw an exception
      nativePinBitmap(bitmap)
    } catch (e: Exception) {
      bitmap.recycle()
      throw Throwables.propagate(e)
    }
    if (!unpooledBitmapsCounter.increase(bitmap)) {
      val bitmapSize = BitmapUtil.getSizeInBytes(bitmap)
      bitmap.recycle()
      val detailMessage =
          String.format(
              Locale.US,
              ("Attempted to pin a bitmap of size %d bytes. The current pool count is %d, the current pool size is %d bytes. The current pool max count is %d, the current pool max size is %d bytes."),
              bitmapSize,
              unpooledBitmapsCounter.count,
              unpooledBitmapsCounter.size,
              unpooledBitmapsCounter.maxCount,
              unpooledBitmapsCounter.maxSize,
          )
      throw TooManyBitmapsException(detailMessage)
    }
    return CloseableReference.of(bitmap, unpooledBitmapsCounter.releaser)
  }

  companion object {
    init {
      ImagePipelineNativeLoader.load()
    }

    @JvmField
    protected val EOI: ByteArray =
        byteArrayOf(JfifUtil.MARKER_FIRST_BYTE.toByte(), JfifUtil.MARKER_EOI.toByte())

    @JvmStatic
    @VisibleForTesting
    fun getBitmapFactoryOptions(
        sampleSize: Int,
        bitmapConfig: Bitmap.Config,
    ): BitmapFactory.Options {
      val options = BitmapFactory.Options()
      // known to improve picture quality at low cost
      options.inDither = true
      options.inPreferredConfig = bitmapConfig
      // Decode the image into a 'purgeable' bitmap that lives on the ashmem heap
      options.inPurgeable = true
      // Enable copy of of bitmap to enable purgeable decoding by filedescriptor
      options.inInputShareable = true
      // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
      options.inSampleSize = sampleSize
      // no known perf difference; allows postprocessing to work
      options.inMutable = true
      return options
    }

    @JvmStatic
    @VisibleForTesting
    fun endsWithEOI(bytesRef: CloseableReference<PooledByteBuffer?>, length: Int): Boolean {
      val buffer = bytesRef.get()
      return length >= 2 &&
          buffer.read(length - 2) == JfifUtil.MARKER_FIRST_BYTE.toByte() &&
          buffer.read(length - 1) == JfifUtil.MARKER_EOI.toByte()
    }

    @JvmStatic @DoNotStrip private external fun nativePinBitmap(bitmap: Bitmap)
  }
}
