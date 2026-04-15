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
import com.facebook.common.internal.Preconditions
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imageutils.JfifUtil
import javax.annotation.concurrent.ThreadSafe

/**
 * Efficient platform decoder that eliminates BitmapPool overhead. Uses GC-managed bitmaps via
 * [NoOpResourceReleaser] instead of pooled allocation, and skips the redundant inJustDecodeBounds
 * pre-pass that [DefaultDecoder] performs.
 *
 * Delegates byte-array decoding to [RawBitmapDecoder] for the actual decode.
 */
@ThreadSafe
class EfficientPlatformDecoder(
    private val rawBitmapDecoder: RawBitmapDecoder,
    private val platformDecoderOptions: PlatformDecoderOptions,
) : PlatformDecoder {

  // Note: regionToDecode is accepted for interface compliance but is not supported.

  // region PlatformDecoder

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

  // Adapted from DefaultDecoder. Now delegates decode to RawBitmapDecoder.
  override fun decodeFromEncodedImageWithColorSpace(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      colorSpace: ColorSpace?,
  ): CloseableReference<Bitmap>? {
    if (!DecodeValidationUtils.validateDimensions(encodedImage, platformDecoderOptions)) return null
    val bytes: ByteArray
    synchronized(encodedImage) { bytes = extractBytes(encodedImage) }
    val sampleSize = encodedImage.sampleSize
    val bitmap =
        rawBitmapDecoder.decode(bytes, 0, bytes.size, bitmapConfig, sampleSize, colorSpace)
            ?: return null
    return CloseableReference.of(bitmap, NoOpResourceReleaser)
  }

  // Adapted from DefaultDecoder. Now delegates decode to RawBitmapDecoder.
  // JPEG partial data handling (EOI tail append for incomplete JPEGs) preserved.
  override fun decodeJPEGFromEncodedImageWithColorSpace(
      encodedImage: EncodedImage,
      bitmapConfig: Bitmap.Config,
      regionToDecode: Rect?,
      length: Int,
      colorSpace: ColorSpace?,
  ): CloseableReference<Bitmap>? {
    if (!DecodeValidationUtils.validateDimensions(encodedImage, platformDecoderOptions)) return null
    val effectiveLength: Int
    val isJpegComplete: Boolean
    val rawBytes: ByteArray
    synchronized(encodedImage) {
      isJpegComplete = encodedImage.isCompleteAt(length)
      effectiveLength = minOf(encodedImage.size, length)
      rawBytes = extractBytes(encodedImage)
    }
    val bytes: ByteArray
    val bytesLength: Int
    if (!isJpegComplete) {
      bytes = ByteArray(effectiveLength + EOI_TAIL.size)
      System.arraycopy(rawBytes, 0, bytes, 0, effectiveLength)
      System.arraycopy(EOI_TAIL, 0, bytes, effectiveLength, EOI_TAIL.size)
      bytesLength = bytes.size
    } else {
      bytes = rawBytes
      bytesLength = effectiveLength
    }
    val sampleSize = encodedImage.sampleSize
    val bitmap =
        rawBitmapDecoder.decode(bytes, 0, bytesLength, bitmapConfig, sampleSize, colorSpace)
            ?: return null
    return CloseableReference.of(bitmap, NoOpResourceReleaser)
  }

  // endregion

  /**
   * Extracts all bytes from the [encodedImage]'s underlying buffer or input stream. Must be called
   * within a synchronized(encodedImage) block.
   */
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
      Preconditions.checkNotNull(encodedImage.inputStream).readBytes()
    }
  }

  // Bitmap lifecycle is GC-managed.
  private object NoOpResourceReleaser : ResourceReleaser<Bitmap> {
    override fun release(value: Bitmap) {
      // NoOp — bitmap lifecycle is GC-managed
    }
  }

  companion object {
    // JPEG end-of-image marker bytes for partial JPEG handling.
    private val EOI_TAIL =
        byteArrayOf(JfifUtil.MARKER_FIRST_BYTE.toByte(), JfifUtil.MARKER_EOI.toByte())
  }
}
