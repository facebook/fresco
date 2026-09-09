/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import java.io.Closeable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Regression tests for drawing after the actual image's owning reference is closed. */
@RunWith(RobolectricTestRunner::class)
class KFrescoVitoDrawableRecycleTest {

  private fun liveBitmap(): Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

  private fun drawableRenderingOwnedBitmap(
      bitmap: Bitmap,
      clearActualImageLayerOnClose: Boolean = true,
  ): KFrescoVitoDrawable {
    val drawable = KFrescoVitoDrawable(clearActualImageLayerOnClose = clearActualImageLayerOnClose)
    drawable.setBounds(0, 0, 100, 100)
    drawable.closeable = Closeable { bitmap.recycle() }
    drawable.actualImageLayer.configure(
        dataModel = BitmapImageDataModel(bitmap),
        bounds = Rect(0, 0, 100, 100),
    )
    return drawable
  }

  @Test
  fun testClosingOwningReference_clearsActualImageLayer() {
    val bitmap = liveBitmap()
    val drawable = drawableRenderingOwnedBitmap(bitmap)
    assertThat(drawable.hasImage()).isTrue()

    drawable.closeable = null

    assertThat(bitmap.isRecycled).isTrue()
    assertThat(drawable.hasImage()).isFalse()
    assertThat(drawable.actualImageLayer.getDataModel()).isNull()
  }

  @Test
  fun testClosingOwningReference_flagOff_preservesOldBehavior() {
    val bitmap = liveBitmap()
    val drawable = drawableRenderingOwnedBitmap(bitmap, clearActualImageLayerOnClose = false)
    assertThat(drawable.hasImage()).isTrue()

    drawable.closeable = null

    assertThat(bitmap.isRecycled).isTrue()
    assertThat(drawable.hasImage()).isTrue()
    assertThat(drawable.actualImageLayer.getDataModel()).isNotNull()
  }

  @Test
  fun testDrawAfterOwningReferenceClosed_drawsNothingAndDoesNotThrow() {
    val bitmap = liveBitmap()
    val drawable = drawableRenderingOwnedBitmap(bitmap)

    drawable.closeable = null

    val canvas = RecordingCanvas()
    drawable.draw(canvas)
    assertThat(canvas.drawCalls).isZero()
  }

  @Test
  fun testReplacementImageAfterOwningReferenceClosed_stillRenders() {
    val first = liveBitmap()
    val drawable = drawableRenderingOwnedBitmap(first)

    val second = liveBitmap()
    drawable.closeable = Closeable { second.recycle() }
    drawable.actualImageLayer.configure(
        dataModel = BitmapImageDataModel(second),
        bounds = Rect(0, 0, 100, 100),
    )

    assertThat(first.isRecycled).isTrue()
    assertThat(second.isRecycled).isFalse()
    assertThat(drawable.hasImage()).isTrue()

    val canvas = RecordingCanvas()
    drawable.draw(canvas)
    assertThat(canvas.drawCalls).isGreaterThan(0)
  }

  private class RecordingCanvas : Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)) {
    var drawCalls = 0
      private set

    override fun drawBitmap(bitmap: Bitmap, left: Float, top: Float, paint: Paint?) {
      drawCalls++
    }

    override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
      drawCalls++
    }

    override fun drawRect(rect: RectF, paint: Paint) {
      drawCalls++
    }

    override fun drawPath(path: Path, paint: Paint) {
      drawCalls++
    }
  }
}
