/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.webpsupport

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.QualityInfo
import java.io.IOException
import java.nio.ByteBuffer

class WebPImageDecoder : ImageDecoder {

  override fun decode(
      encodedImage: EncodedImage,
      length: Int,
      qualityInfo: QualityInfo,
      options: ImageDecodeOptions,
  ): CloseableImage? {
    val bytesRef = encodedImage.byteBufferRef
    checkNotNull(bytesRef)
    try {
      val input = bytesRef.get()
      val buffer = input.byteBuffer
      if (buffer != null) {
        return decodeByteBuffer(buffer, encodedImage, length, qualityInfo, options)
      }
    } finally {
      CloseableReference.closeSafely(bytesRef)
    }

    return decodeInputStream(encodedImage, length, qualityInfo, options)
  }

  companion object {
    private fun decodeByteBuffer(
        byteBuffer: ByteBuffer,
        encodedImage: EncodedImage,
        length: Int,
        qualityInfo: QualityInfo,
        options: ImageDecodeOptions,
    ): CloseableImage? {
      val bitmap = WebpBitmapFactoryImpl.hookDecodeByteArray(byteBuffer.array(), 0, length)
      return bitmapToCloseableImage(bitmap, encodedImage, qualityInfo)
    }

    private fun decodeInputStream(
        encodedImage: EncodedImage,
        length: Int,
        qualityInfo: QualityInfo,
        options: ImageDecodeOptions,
    ): CloseableImage? {
      try {
        encodedImage.inputStreamOrThrow.use { `is` ->
          val bitmap = WebpBitmapFactoryImpl.hookDecodeStream(`is`, null, null)
          return bitmapToCloseableImage(bitmap, encodedImage, qualityInfo)
        }
      } catch (e: IOException) {
        throw RuntimeException("Error while decoding WebP", e)
      }
    }

    private fun bitmapToCloseableImage(
        bitmap: Bitmap?,
        encodedImage: EncodedImage,
        qualityInfo: QualityInfo,
    ): CloseableImage? {
      if (bitmap == null) {
        return null
      }
      val bitmapRef = CloseableReference.of(bitmap, SimpleBitmapReleaser.getInstance())
      return CloseableStaticBitmap.of(bitmapRef, qualityInfo, encodedImage.rotationAngle)
    }
  }
}
