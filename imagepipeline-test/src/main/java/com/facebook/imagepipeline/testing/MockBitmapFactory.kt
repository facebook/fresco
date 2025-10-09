/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing

import android.graphics.Bitmap
import com.facebook.imageutils.BitmapUtil
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Helper class for creating bitmap mocks in tests. */
object MockBitmapFactory {

  const val DEFAULT_BITMAP_WIDTH: Int = 3
  const val DEFAULT_BITMAP_HEIGHT: Int = 4
  const val DEFAULT_BITMAP_PIXELS: Int = DEFAULT_BITMAP_WIDTH * DEFAULT_BITMAP_HEIGHT
  @get:JvmStatic
  val DEFAULT_BITMAP_SIZE: Int =
      bitmapSize(DEFAULT_BITMAP_WIDTH, DEFAULT_BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)

  @JvmStatic
  fun createForSize(size: Int, config: Bitmap.Config): Bitmap {
    require(size % BitmapUtil.getPixelSizeForBitmapConfig(config) == 0)
    return create(1, size / BitmapUtil.getPixelSizeForBitmapConfig(config), config)
  }

  @JvmStatic
  @JvmOverloads
  fun create(
      width: Int = DEFAULT_BITMAP_WIDTH,
      height: Int = DEFAULT_BITMAP_HEIGHT,
      config: Bitmap.Config = Bitmap.Config.ARGB_8888,
  ): Bitmap {
    require(width > 0)
    require(height > 0)
    checkNotNull(config)
    val bitmap = mock<Bitmap>()
    whenever(bitmap.width).thenReturn(width)
    whenever(bitmap.height).thenReturn(height)
    whenever(bitmap.config).thenReturn(config)
    whenever(bitmap.isMutable).thenReturn(true)
    whenever(bitmap.rowBytes).thenReturn(width * BitmapUtil.getPixelSizeForBitmapConfig(config))
    whenever(bitmap.byteCount).thenReturn(bitmapSize(width, height, config))
    return bitmap
  }

  @JvmStatic
  fun bitmapSize(width: Int, height: Int, config: Bitmap.Config): Int =
      BitmapUtil.getSizeInByteForBitmap(width, height, config)
}
