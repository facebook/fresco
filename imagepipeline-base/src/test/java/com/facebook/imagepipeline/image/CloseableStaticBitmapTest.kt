/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import android.graphics.Bitmap
import android.media.ExifInterface
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CloseableStaticBitmapTest {
  private lateinit var bitmap: Bitmap
  private lateinit var closeableStaticBitmap: CloseableStaticBitmap

  @Before
  fun setup() {
    bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
    val releaser: ResourceReleaser<Bitmap> = SimpleBitmapReleaser.getInstance()
    closeableStaticBitmap =
        CloseableStaticBitmap.of(
            bitmap,
            releaser,
            ImmutableQualityInfo.FULL_QUALITY,
            0,
            ExifInterface.ORIENTATION_NORMAL)
  }

  @Test
  fun testWidthAndHeight() {
    assertThat(closeableStaticBitmap.width).isEqualTo(WIDTH)
    assertThat(closeableStaticBitmap.height).isEqualTo(HEIGHT)
  }

  @Test
  fun testWidthAndHeightWithRotatedImage() {
    // Reverse width and height as the rotation angle should put them back again
    bitmap = Bitmap.createBitmap(HEIGHT, WIDTH, Bitmap.Config.ARGB_8888)
    val releaser: ResourceReleaser<Bitmap> = SimpleBitmapReleaser.getInstance()
    closeableStaticBitmap =
        CloseableStaticBitmap.of(
            bitmap,
            releaser,
            ImmutableQualityInfo.FULL_QUALITY,
            90,
            ExifInterface.ORIENTATION_ROTATE_90)

    assertThat(closeableStaticBitmap.width).isEqualTo(WIDTH)
    assertThat(closeableStaticBitmap.height).isEqualTo(HEIGHT)
  }

  @Test
  fun testWidthAndHeightWithInvertedOrientationImage() {
    // Reverse width and height as the inverted orientation should put them back again
    bitmap = Bitmap.createBitmap(HEIGHT, WIDTH, Bitmap.Config.ARGB_8888)
    val releaser: ResourceReleaser<Bitmap> = SimpleBitmapReleaser.getInstance()
    closeableStaticBitmap =
        CloseableStaticBitmap.of(
            bitmap,
            releaser,
            ImmutableQualityInfo.FULL_QUALITY,
            0,
            ExifInterface.ORIENTATION_TRANSPOSE)

    assertThat(closeableStaticBitmap.width).isEqualTo(WIDTH)
    assertThat(closeableStaticBitmap.height).isEqualTo(HEIGHT)
  }

  @Test
  fun testClose() {
    closeableStaticBitmap.close()
    assertThat(closeableStaticBitmap.isClosed).isTrue()
    assertThat(bitmap.isRecycled).isTrue()
  }

  @Test
  fun testConvert() {
    val ref: CloseableReference<Bitmap> = closeableStaticBitmap.convertToBitmapReference()
    assertThat(ref.get()).isSameAs(bitmap)
    assertThat(closeableStaticBitmap.isClosed).isTrue()
  }

  @Test
  fun testCloneUnderlyingBitmapReference() {
    val clonedBitmapReference: CloseableReference<Bitmap>? =
        closeableStaticBitmap.cloneUnderlyingBitmapReference()
    assertThat(clonedBitmapReference).isNotNull()
    assertThat(clonedBitmapReference?.get()).isEqualTo(bitmap)
  }

  @Test
  fun testCloneUnderlyingBitmapReference_whenBitmapClosed_thenReturnNull() {
    closeableStaticBitmap.close()
    val clonedBitmapReference: CloseableReference<Bitmap>? =
        closeableStaticBitmap.cloneUnderlyingBitmapReference()
    assertThat(clonedBitmapReference).isNull()
  }

  companion object {
    private const val WIDTH = 10
    private const val HEIGHT = 5
  }
}
