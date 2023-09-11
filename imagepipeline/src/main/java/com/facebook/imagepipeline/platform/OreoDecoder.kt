/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.BitmapFactory.Options
import android.os.Build
import androidx.core.util.Pools
import com.facebook.imagepipeline.memory.BitmapPool
import com.facebook.imageutils.BitmapUtil
import java.nio.ByteBuffer
import javax.annotation.concurrent.ThreadSafe

/** Bitmap decoder for ART VM (Android O and up). */
@TargetApi(Build.VERSION_CODES.O)
@ThreadSafe
class OreoDecoder(
    bitmapPool: BitmapPool,
    decodeBuffers: Pools.Pool<ByteBuffer>,
    private val useOutConfig: Boolean,
    fixReadingOptions: Boolean,
    avoidPool: Boolean,
) : DefaultDecoder(bitmapPool, decodeBuffers, fixReadingOptions, avoidPool) {

  override fun getBitmapSize(width: Int, height: Int, options: Options): Int {
    if (useOutConfig) {
      return BitmapUtil.getSizeInByteForBitmap(
          width, height, options.outConfig ?: Bitmap.Config.ARGB_8888)
    }
    // If the color is wide gamut but the Bitmap Config doesn't use 8 bytes per pixel, the size of
    // the bitmap
    // needs to be computed manually to get the correct size.
    return if (hasColorGamutMismatch(options)) {
      width * height * 8
    } else {
      BitmapUtil.getSizeInByteForBitmap(
          width, height, options.inPreferredConfig ?: Bitmap.Config.ARGB_8888)
    }
  }

  companion object {
    /** Check if the color space has a wide color gamut and is consistent with the Bitmap config */
    private fun hasColorGamutMismatch(options: Options): Boolean =
        options.outColorSpace != null &&
            options.outColorSpace.isWideGamut &&
            options.inPreferredConfig != Bitmap.Config.RGBA_F16
  }
}
