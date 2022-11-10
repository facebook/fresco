/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory

import android.graphics.Bitmap
import android.graphics.Color
import com.facebook.common.logging.FLog
import com.facebook.imageutils.BitmapUtil

open class BitmapPoolBackend : LruBucketsPoolBackend<Bitmap>() {

  override fun put(bitmap: Bitmap) {
    if (isReusable(bitmap)) {
      super.put(bitmap)
    }
  }

  override fun get(size: Int): Bitmap? {
    val bitmap = super.get(size)
    if (bitmap != null && isReusable(bitmap)) {
      bitmap.eraseColor(Color.TRANSPARENT)
      return bitmap
    }
    return null
  }

  override fun getSize(bitmap: Bitmap): Int = BitmapUtil.getSizeInBytes(bitmap)

  protected fun isReusable(bitmap: Bitmap?): Boolean {
    if (bitmap == null) {
      return false
    }
    if (bitmap.isRecycled) {
      FLog.wtf(TAG, "Cannot reuse a recycled bitmap: %s", bitmap)
      return false
    }
    if (!bitmap.isMutable) {
      FLog.wtf(TAG, "Cannot reuse an immutable bitmap: %s", bitmap)
      return false
    }
    return true
  }

  companion object {
    private const val TAG = "BitmapPoolBackend"
  }
}
