/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.util.DisplayMetrics
import com.facebook.drawee.drawable.RoundedBitmapDrawable
import com.facebook.drawee.drawable.RoundedColorDrawable
import com.facebook.drawee.drawable.RoundedNinePatchDrawable
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.RoundingOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoundingUtilsTest {

  private lateinit var resources: Resources
  private lateinit var displayMetrics: DisplayMetrics

  @Before
  fun setup() {
    resources = mock()
    displayMetrics = DisplayMetrics()
    whenever(resources.displayMetrics).thenReturn(displayMetrics)
  }

  @Test
  fun testRoundedDrawablesWithoutBorder_withBitmap_withNotAlreadyRounded_thenReturnBitmapDrawable() {
    val roundingUtils = RoundingUtils
    val bitmap = mock<Bitmap>()
    val drawable =
        roundingUtils.roundedDrawable(resources, bitmap, null, RoundingOptions.asCircle())
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable::class.java)
    assertThat((drawable as RoundedBitmapDrawable).isCircle).isTrue
  }

  @Test
  fun testRoundedDrawablesWithBorder_withBitmap_withNotAlreadyRounded_thenReturnBitmapDrawable() {
    val roundingUtils = RoundingUtils
    val bitmap = mock<Bitmap>()
    val borderOptions = BorderOptions.create(Color.YELLOW, 10f)
    val drawable =
        roundingUtils.roundedDrawable(resources, bitmap, borderOptions, RoundingOptions.asCircle())
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable::class.java)
    assertThat((drawable as RoundedBitmapDrawable).borderWidth).isEqualTo(borderOptions.width)
    assertThat(drawable.borderColor).isEqualTo(borderOptions.color)
    assertThat(drawable.isCircle).isTrue
  }

  @Test
  fun testRoundedDrawablesWithoutBorder_withDrawable_thenReturnBitmapDrawable() {
    val roundingUtils = RoundingUtils
    val roundingOptions = RoundingOptions.asCircle()
    var drawable: Drawable = mock<BitmapDrawable>()
    var result = roundingUtils.roundedDrawable(resources, drawable, null, roundingOptions)
    assertThat(result).isNotNull
    assertThat(result).isInstanceOf(RoundedBitmapDrawable::class.java)
    assertThat((result as RoundedBitmapDrawable).isCircle).isTrue

    drawable = mock<ColorDrawable>()
    result = roundingUtils.roundedDrawable(resources, drawable, null, roundingOptions)
    assertThat(result).isNotNull
    assertThat(result).isInstanceOf(RoundedColorDrawable::class.java)
    assertThat((result as RoundedColorDrawable).isCircle).isTrue

    drawable = mock<NinePatchDrawable>()
    result = roundingUtils.roundedDrawable(resources, drawable, null, roundingOptions)
    assertThat(result).isNotNull
    assertThat(result).isInstanceOf(RoundedNinePatchDrawable::class.java)
    assertThat((result as RoundedNinePatchDrawable).isCircle).isTrue
  }

  @Test
  fun testRoundedDrawablesWithBorder_withDrawable_thenReturnBitmapDrawable() {
    val roundingUtils = RoundingUtils
    var drawable: Drawable = mock<BitmapDrawable>()
    val borderOptions = BorderOptions.create(Color.YELLOW, 10f)
    var result =
        roundingUtils.roundedDrawable(
            resources, drawable, borderOptions, RoundingOptions.asCircle())
    assertThat(result).isNotNull
    assertThat(result).isInstanceOf(RoundedBitmapDrawable::class.java)
    assertThat((result as RoundedBitmapDrawable).borderWidth).isEqualTo(borderOptions.width)
    assertThat(result.borderColor).isEqualTo(borderOptions.color)
    assertThat(result.isCircle).isTrue

    drawable = mock<ColorDrawable>()
    result =
        roundingUtils.roundedDrawable(
            resources, drawable, borderOptions, RoundingOptions.asCircle())
    assertThat(result).isNotNull
    assertThat(result).isInstanceOf(RoundedColorDrawable::class.java)
    assertThat((result as RoundedColorDrawable).borderWidth).isEqualTo(borderOptions.width)
    assertThat(result.borderColor).isEqualTo(borderOptions.color)
    assertThat(result.isCircle).isTrue

    drawable = mock<NinePatchDrawable>()
    result =
        roundingUtils.roundedDrawable(
            resources, drawable, borderOptions, RoundingOptions.asCircle())
    assertThat(result).isNotNull
    assertThat(result).isInstanceOf(RoundedNinePatchDrawable::class.java)
    assertThat((result as RoundedNinePatchDrawable).borderWidth).isEqualTo(borderOptions.width)
    assertThat(result.borderColor).isEqualTo(borderOptions.color)
    assertThat(result.isCircle).isTrue
  }
}
