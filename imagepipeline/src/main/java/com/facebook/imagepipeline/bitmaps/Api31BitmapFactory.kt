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
import com.facebook.imagepipeline.core.CloseableReferenceFactory
import com.facebook.imagepipeline.memory.BitmapPool
import javax.annotation.concurrent.ThreadSafe

/** Bitmap factory for ART VM (Lollipop and up). */
@TargetApi(Build.VERSION_CODES.S)
@ThreadSafe
class Api31BitmapFactory(
    bitmapPool: BitmapPool,
    closeableReferenceFactory: CloseableReferenceFactory,
    private val useAshmem: Boolean,
) : ArtBitmapFactory(bitmapPool, closeableReferenceFactory) {

  override fun createBackingBitmap(width: Int, height: Int, bitmapConfig: Bitmap.Config): Bitmap {
    val bitmap = super.createBackingBitmap(width, height, bitmapConfig)
    return if (useAshmem) {
      bitmap.asShared()
    } else {
      bitmap
    }
  }
}
