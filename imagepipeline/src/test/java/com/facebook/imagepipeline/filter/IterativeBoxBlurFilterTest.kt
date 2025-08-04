/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.graphics.Bitmap
import com.facebook.imageutils.BitmapUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class IterativeBoxBlurFilterTest {
  private val bitmapDimension = BitmapUtil.MAX_BITMAP_DIMENSION.toInt()
  private val bitmap =
      Bitmap.createBitmap(bitmapDimension, bitmapDimension, Bitmap.Config.ARGB_8888)

  @Test
  fun testBitmapBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(bitmap, 1, 4)
    assertThat(bitmap).isNotNull()
    assertThat(bitmap.width.toLong()).isEqualTo(bitmapDimension.toLong())
    assertThat(bitmap.height.toLong()).isEqualTo(bitmapDimension.toLong())
    assertThat(bitmap.config).isEqualTo(Bitmap.Config.ARGB_8888)
  }

  @Test
  fun maxRadiusBitmapBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(bitmap, 1, RenderScriptBlurFilter.BLUR_MAX_RADIUS)
    assertThat(bitmap).isNotNull()
    assertThat(bitmap.width.toLong()).isEqualTo(bitmapDimension.toLong())
    assertThat(bitmap.height.toLong()).isEqualTo(bitmapDimension.toLong())
    assertThat(bitmap.config).isEqualTo(Bitmap.Config.ARGB_8888)
  }

  @Test(expected = IllegalArgumentException::class)
  fun invalidNegativeRadiusBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(bitmap, 1, -1)
  }

  @Test(expected = IllegalArgumentException::class)
  fun invalidZeroRadiusBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(bitmap, 1, 0)
  }

  @Test(expected = IllegalArgumentException::class)
  fun invalidBigRadiusBlurInPlace() {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(
        bitmap, 1, RenderScriptBlurFilter.BLUR_MAX_RADIUS + 1)
  }
}
