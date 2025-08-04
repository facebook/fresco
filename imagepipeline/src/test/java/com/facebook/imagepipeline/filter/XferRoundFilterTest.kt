/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.graphics.Bitmap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class XferRoundFilterTest {
  private val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

  @Test
  fun whenValidBitmap_thenRoundingReturnsWithoutError() {
    val destBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    assertThat(bitmap).isNotNull()
    XferRoundFilter.xferRoundBitmap(destBitmap, bitmap, true)
    assertThat(destBitmap).isNotNull()
    assertThat(destBitmap.config).isEqualTo(bitmap.config)
    assertThat(destBitmap.height.toLong()).isEqualTo(bitmap.height.toLong())
    assertThat(destBitmap.width.toLong()).isEqualTo(bitmap.width.toLong())
    destBitmap.recycle()
  }

  @Test(expected = NullPointerException::class)
  fun whenNullDestBitmap_thenRoundingReturnsWithError() {
    assertThat(bitmap).isNotNull()
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")
    XferRoundFilter.xferRoundBitmap(null as Bitmap, bitmap, true)
  }

  @Test(expected = NullPointerException::class)
  fun whenNullSrcBitmap_thenRoundingReturnsWithError() {
    val destBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")
    XferRoundFilter.xferRoundBitmap(destBitmap, null as Bitmap, true)
  }
}
