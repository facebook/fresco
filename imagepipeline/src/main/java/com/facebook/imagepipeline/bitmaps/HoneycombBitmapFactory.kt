/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imagepipeline.core.CloseableReferenceFactory
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.platform.PlatformDecoder
import javax.annotation.concurrent.ThreadSafe

/** Factory implementation for Honeycomb through Kitkat */
@ThreadSafe
class HoneycombBitmapFactory(
    private val jpegGenerator: EmptyJpegGenerator,
    private val purgeableDecoder: PlatformDecoder,
    private val closeableReferenceFactory: CloseableReferenceFactory
) : PlatformBitmapFactory() {

  private var immutableBitmapFallback = false

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the [android.graphics.Bitmap.Config] used to create the decoded Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  override fun createBitmapInternal(
      width: Int,
      height: Int,
      bitmapConfig: Bitmap.Config
  ): CloseableReference<Bitmap> {
    if (immutableBitmapFallback) {
      return createFallbackBitmap(width, height, bitmapConfig)
    }
    val jpgRef = jpegGenerator.generate(width.toShort(), height.toShort())
    return try {
      val encodedImage = EncodedImage(jpgRef)
      encodedImage.imageFormat = DefaultImageFormats.JPEG
      try {
        val bitmapRef =
            purgeableDecoder.decodeJPEGFromEncodedImage(
                encodedImage, bitmapConfig, null, jpgRef.get().size())
        checkNotNull(bitmapRef)
        if (!bitmapRef.get().isMutable) {
          CloseableReference.closeSafely(bitmapRef)
          immutableBitmapFallback = true
          FLog.wtf(TAG, "Immutable bitmap returned by decoder")
          // On some devices (Samsung GT-S7580) the returned bitmap can be immutable, in that case
          // let's jut use Bitmap.createBitmap() to hopefully create a mutable one.
          return createFallbackBitmap(width, height, bitmapConfig)
        }
        bitmapRef.get().setHasAlpha(true)
        bitmapRef.get().eraseColor(Color.TRANSPARENT)
        bitmapRef
      } finally {
        EncodedImage.closeSafely(encodedImage)
      }
    } finally {
      jpgRef.close()
    }
  }

  private fun createFallbackBitmap(
      width: Int,
      height: Int,
      bitmapConfig: Bitmap.Config
  ): CloseableReference<Bitmap> =
      closeableReferenceFactory.create(
          Bitmap.createBitmap(width, height, bitmapConfig), SimpleBitmapReleaser.getInstance())

  companion object {
    private val TAG = HoneycombBitmapFactory::class.java.simpleName
  }
}
