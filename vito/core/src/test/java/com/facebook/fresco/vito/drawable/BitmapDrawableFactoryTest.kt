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
import android.util.DisplayMetrics
import com.facebook.drawee.drawable.RoundedBitmapDrawable
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapDrawableFactoryTest {

  private val imageExtrasRounded: Map<String, Any> = mapOf("is_rounded" to true)
  private val imageExtrasNotRounded: Map<String, Any> = mapOf("is_rounded" to false)

  private lateinit var resources: Resources
  private lateinit var displayMetrics: DisplayMetrics
  private lateinit var drawableFactory: ImageOptionsDrawableFactory

  @Before
  fun setup() {
    resources = mock()
    displayMetrics = DisplayMetrics()
    whenever(resources.displayMetrics).thenReturn(displayMetrics)
    drawableFactory = BitmapDrawableFactory()
  }

  @Test
  fun testCreateDrawable_whenImageUnknown_thenReturnNull() {
    val closeableImage = mock<CloseableImage>()
    val options = mock<ImageOptions>()
    val drawable = drawableFactory.createDrawable(resources, closeableImage, options)
    assertThat(drawable).isNull()
  }

  @Test
  fun testCreateDrawable_whenImageIsStaticBitmap_thenReturnBitmapDrawable() {
    val closeableImage = mock<CloseableStaticBitmap>()
    val bitmap = mock<Bitmap>()
    whenever(closeableImage.underlyingBitmap).thenReturn(bitmap)
    val options = mock<ImageOptions>()
    val drawable = drawableFactory.createDrawable(resources, closeableImage, options)
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(BitmapDrawable::class.java)
  }

  @Test
  fun testCreateDrawable_whenRoundAsCircleAndJavaRounding_thenReturnRoundedBitmapDrawable() {
    val closeableImage = mock<CloseableStaticBitmap>()
    val bitmap = mock<Bitmap>()
    whenever(closeableImage.underlyingBitmap).thenReturn(bitmap)
    val options = mock<ImageOptions>()
    whenever(options.roundingOptions).thenReturn(RoundingOptions.asCircle())
    val factory = BitmapDrawableFactory()
    val drawable = factory.createDrawable(resources, closeableImage, options)
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable::class.java)
  }

  @Test
  fun testCreateDrawable_whenRoundAsCircleAndNativeRounding_thenReturnBitmapDrawable() {
    val closeableImage = mock<CloseableStaticBitmap>()
    val bitmap = mock<Bitmap>()
    whenever(closeableImage.underlyingBitmap).thenReturn(bitmap)
    whenever(closeableImage.extras).thenReturn(imageExtrasRounded)
    val options = mock<ImageOptions>()
    whenever(options.roundingOptions).thenReturn(RoundingOptions.asCircle())
    val drawable = drawableFactory.createDrawable(resources, closeableImage, options)
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(BitmapDrawable::class.java)
  }

  @Test
  fun testCreateDrawable_whenRoundWithCornerRadius_thenReturnBitmapDrawable() {
    val closeableImage = mock<CloseableStaticBitmap>()
    val bitmap = mock<Bitmap>()
    whenever(closeableImage.underlyingBitmap).thenReturn(bitmap)
    val options = mock<ImageOptions>()
    whenever(options.roundingOptions).thenReturn(RoundingOptions.forCornerRadiusPx(123f))
    whenever(closeableImage.extras).thenReturn(imageExtrasNotRounded)
    val drawable = drawableFactory.createDrawable(resources, closeableImage, options)
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable::class.java)
    assertThat((drawable as RoundedBitmapDrawable?)?.radii)
        .isEqualTo(floatArrayOf(123f, 123f, 123f, 123f, 123f, 123f, 123f, 123f))
  }

  @Test
  fun testCreateDrawable_whenRoundWithCornerRadii_thenReturnBitmapDrawable() {
    val closeableImage = mock<CloseableStaticBitmap>()
    val bitmap = mock<Bitmap>()
    whenever(closeableImage.underlyingBitmap).thenReturn(bitmap)
    val options = mock<ImageOptions>()
    whenever(options.roundingOptions).thenReturn(RoundingOptions.forCornerRadii(1f, 2f, 3f, 4f))
    whenever(closeableImage.extras).thenReturn(imageExtrasNotRounded)
    val drawable = drawableFactory.createDrawable(resources, closeableImage, options)
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(RoundedBitmapDrawable::class.java)
    assertThat((drawable as RoundedBitmapDrawable?)?.radii)
        .isEqualTo(floatArrayOf(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f))
  }

  @Test
  fun testCreateDrawable_whenAlreadyRoundedWithBorder_thenReturnBitmapDrawable() {
    val closeableImage = mock<CloseableStaticBitmap>()
    val bitmap = mock<Bitmap>()
    whenever(closeableImage.underlyingBitmap).thenReturn(bitmap)
    whenever(closeableImage.extras).thenReturn(imageExtrasRounded)
    val options = mock<ImageOptions>()
    val borderOptions = BorderOptions.create(Color.YELLOW, 10f)
    whenever(options.borderOptions).thenReturn(borderOptions)
    whenever(options.roundingOptions)
        .thenReturn(RoundingOptions.asCircle(antiAliasing = false, forceRoundAtDecode = false))
    val drawable = drawableFactory.createDrawable(resources, closeableImage, options)
    assertThat(drawable).isNotNull
    assertThat(drawable).isInstanceOf(CircularBorderBitmapDrawable::class.java)
    assertThat((drawable as CircularBorderBitmapDrawable?)?.border).isEqualTo(borderOptions)
  }
}
