/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Rect
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imageutils.JfifUtil
import java.io.InputStream
import javax.annotation.concurrent.ThreadSafe

/** Pool-free platform decoder using GC-managed bitmaps. Delegates to [RawBitmapDecoder]. */
@ThreadSafe
class EfficientPlatformDecoder(
    private val rawBitmapDecoder: RawBitmapDecoder,
    private val platformDecoderOptions: PlatformDecoderOptions,
) : PlatformDecoder {

  // regionToDecode is not supported; parameter exists for interface compliance.

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

  override fun decodeFromEncodedImageWithColorSpace(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
  ): CloseableReference<Bitmap>? {
    if (!DecodeValidationUtils.validateDimensions(encodedImage, platformDecoderOptions)) return null
    val sampleSize = encodedImage.sampleSize
    val bytes: ByteArray
    synchronized(encodedImage) { bytes = extractBytes(encodedImage) }
    val bitmap =
        decodeWithRetry(bytes, 0, bytes.size, bitmapConfig, sampleSize, colorSpace) ?: return null
    return CloseableReference.of(bitmap, NoOpResourceReleaser)
  }

  override fun decodeJPEGFromEncodedImageWithColorSpace(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      length: Int,
      colorSpace: ColorSpace?,
  ): CloseableReference<Bitmap>? {
    if (!DecodeValidationUtils.validateDimensions(encodedImage, platformDecoderOptions)) return null
    val sampleSize = encodedImage.sampleSize
    val isJpegComplete: Boolean
    val effectiveLength: Int
    val encodedImageSize: Int
    synchronized(encodedImage) {
      isJpegComplete = encodedImage.isCompleteAt(length)
      encodedImageSize = encodedImage.size
      effectiveLength = minOf(encodedImageSize, length)
    }
    if (effectiveLength <= 0) return null

    // Single allocation: pre-size for EOI if incomplete, exact size if complete.
    // Read directly from PooledByteBuffer — no intermediate rawBytes array.
    val bytes: ByteArray
    val bytesLength: Int
    if (!isJpegComplete) {
      bytes = ByteArray(effectiveLength + EOI_TAIL.size)
      bytesLength = bytes.size
    } else {
      bytes = ByteArray(effectiveLength)
      bytesLength = effectiveLength
    }

    synchronized(encodedImage) {
      val ref = encodedImage.getByteBufferRef()
      if (ref != null) {
        try {
          ref.get().read(0, bytes, 0, effectiveLength)
        } finally {
          ref.close()
        }
      } else {
        val inputStream: InputStream = encodedImage.inputStream ?: return null
        var offset = 0
        while (offset < effectiveLength) {
          val bytesRead = inputStream.read(bytes, offset, effectiveLength - offset)
          if (bytesRead <= 0) break
          offset += bytesRead
        }
      }
    }

    if (!isJpegComplete) {
      System.arraycopy(EOI_TAIL, 0, bytes, effectiveLength, EOI_TAIL.size)
    }

    val bitmap =
        decodeWithRetry(bytes, 0, bytesLength, bitmapConfig, sampleSize, colorSpace) ?: return null
    return CloseableReference.of(bitmap, NoOpResourceReleaser)
  }

  private fun decodeWithRetry(
      bytes: ByteArray,
      offset: Int,
      length: Int,
      bitmapConfig: Bitmap.Config,
      sampleSize: Int,
      colorSpace: ColorSpace?,
  ): Bitmap? {
    val retryOnFail = bitmapConfig != Bitmap.Config.ARGB_8888
    return try {
      rawBitmapDecoder.decode(bytes, offset, length, bitmapConfig, sampleSize, colorSpace)
    } catch (re: RuntimeException) {
      if (retryOnFail) {
        rawBitmapDecoder.decode(
            bytes,
            offset,
            length,
            Bitmap.Config.ARGB_8888,
            sampleSize,
            colorSpace,
        )
      } else {
        throw re
      }
    }
  }

  private fun extractBytes(encodedImage: EncodedImage): ByteArray {
    val ref = encodedImage.getByteBufferRef()
    return if (ref != null) {
      try {
        val buffer = ref.get()
        ByteArray(buffer.size()).also { buffer.read(0, it, 0, buffer.size()) }
      } finally {
        ref.close()
      }
    } else {
      encodedImage.inputStream?.readBytes() ?: ByteArray(0)
    }
  }

  private object NoOpResourceReleaser : ResourceReleaser<Bitmap> {
    override fun release(value: Bitmap) {
      // NoOp
    }
  }

  companion object {
    private val EOI_TAIL =
        byteArrayOf(JfifUtil.MARKER_FIRST_BYTE.toByte(), JfifUtil.MARKER_EOI.toByte())
  }
}
