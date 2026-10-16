/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageWithTransformationAndBorderRendererTest {
  @Test
  fun fixesDisabled_keepsImageClip() {
    assertThat(imageClipCount(RectF(-1f, -1f, 9f, 7f), false)).isEqualTo(1)
  }

  @Test
  fun fixesEnabled_strictContainmentSkipsImageClip() {
    assertThat(imageClipCount(RectF(-0.25f, -0.25f, 8.25f, 6.25f), true)).isZero()
  }

  @Test
  fun fixesEnabled_keepsImageClipForCoincidentEdgesAndCrop() {
    for (rect in
        listOf(
            RectF(0f, 0f, 8f, 6f),
            RectF(0f, -1f, 9f, 7f),
            RectF(-1f, 0f, 9f, 7f),
            RectF(-1f, -1f, 8f, 7f),
            RectF(-1f, -1f, 9f, 6f),
            RectF(1f, 1f, 7f, 5f),
        )) {
      assertThat(imageClipCount(rect, true)).isEqualTo(1)
    }
  }

  private fun imageClipCount(imageBounds: RectF, enabled: Boolean): Int {
    val image = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)
    val output = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
    val canvas =
        object : Canvas(output) {
          var imageClips = 0

          override fun clipRect(rect: RectF): Boolean {
            imageClips++
            return super.clipRect(rect)
          }
        }
    val saveCount = canvas.saveCount
    ImageWithTransformationAndBorderRenderer.createRenderCommand(
        BitmapImageDataModel(image),
        null,
        null,
        Matrix().apply {
          setRectToRect(RectF(0f, 0f, 8f, 6f), imageBounds, Matrix.ScaleToFit.FILL)
        },
        Rect(0, 0, 8, 6),
        Paint(),
        skipRedundantImageClip = enabled,
    )(canvas)
    assertThat(canvas.saveCount).isEqualTo(saveCount)
    image.recycle()
    output.recycle()
    return canvas.imageClips
  }
}
