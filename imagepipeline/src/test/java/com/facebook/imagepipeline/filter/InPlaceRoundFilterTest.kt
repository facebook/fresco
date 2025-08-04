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
class InPlaceRoundFilterTest {
  private val BITMAP_DIMENSION = BitmapUtil.MAX_BITMAP_DIMENSION.toInt()

  @Test
  fun whenMaximumSizeBitmap_thenRoundingReturnsWithoutError() {
    val bitmap = Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.ARGB_8888)
    assertThat(bitmap).isNotNull()
    InPlaceRoundFilter.roundBitmapInPlace(bitmap)
    bitmap.recycle()
  }

  @Test
  fun whenOnePixelBitmap_thenRoundingReturnsWithoutError() {
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    InPlaceRoundFilter.roundBitmapInPlace(bitmap)
    bitmap.recycle()
  }

  @Test(expected = NullPointerException::class)
  fun whenNullBitmap_thenRoundingReturnsWithError() {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")
    InPlaceRoundFilter.roundBitmapInPlace(null as Bitmap)
  }

  @Test(expected = IllegalArgumentException::class)
  fun whenEmptyBitmap_thenRoundingReturnsWithError() {
    val bitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888)
    InPlaceRoundFilter.roundBitmapInPlace(bitmap)
    bitmap.recycle()
  }

  @Test(expected = IllegalArgumentException::class)
  fun whenTooBigBitmap_thenRoundingReturnsWithError() {
    val bitmap =
        Bitmap.createBitmap(BITMAP_DIMENSION + 1, BITMAP_DIMENSION + 1, Bitmap.Config.ARGB_8888)
    InPlaceRoundFilter.roundBitmapInPlace(bitmap)
    bitmap.recycle()
  }
}
