/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.core.CloseableReferenceFactory
import com.facebook.imagepipeline.memory.BitmapPool
import com.facebook.imageutils.BitmapUtil
import javax.annotation.concurrent.ThreadSafe

/** Bitmap factory for ART VM (Lollipop and up). */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@ThreadSafe
class ArtBitmapFactory(
    private val bitmapPool: BitmapPool,
    private val closeableReferenceFactory: CloseableReferenceFactory
) : PlatformBitmapFactory() {
  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the [android.graphics.Bitmap.Config] used to create the decoded Bitmap
   * @return a reference to the bitmap
   * @exception java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  override fun createBitmapInternal(
      width: Int,
      height: Int,
      bitmapConfig: Bitmap.Config
  ): CloseableReference<Bitmap> {
    val sizeInBytes = BitmapUtil.getSizeInByteForBitmap(width, height, bitmapConfig)
    val bitmap = bitmapPool[sizeInBytes]
    check(
        bitmap.allocationByteCount >=
            width * height * BitmapUtil.getPixelSizeForBitmapConfig(bitmapConfig))
    bitmap.reconfigure(width, height, bitmapConfig)
    return closeableReferenceFactory.create(bitmap, bitmapPool)
  }
}
