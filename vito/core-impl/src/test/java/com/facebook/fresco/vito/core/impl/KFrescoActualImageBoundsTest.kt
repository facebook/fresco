/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.fresco.vito.renderer.CanvasTransformation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for [KFrescoVitoDrawable.getActualImageBounds]. */
@RunWith(RobolectricTestRunner::class)
class KFrescoActualImageBoundsTest {

  @Test
  fun testGetActualImageBounds_flagOff_throws() {
    val drawable = KFrescoVitoDrawable()

    assertThatThrownBy { drawable.getActualImageBounds(RectF()) }
        .isInstanceOf(UnsupportedOperationException::class.java)
  }

  @Test
  fun testGetActualImageBounds_flagOnNoImage_leavesBoundsUntouched() {
    val drawable = KFrescoVitoDrawable(fixActualImageBounds = true)
    val outBounds = RectF(1f, 2f, 3f, 4f)

    drawable.getActualImageBounds(outBounds)

    assertThat(outBounds).isEqualTo(RectF(1f, 2f, 3f, 4f))
  }

  @Test
  fun testGetActualImageBounds_flagOnWithImage_mapsModelRectThroughTransform() {
    val drawable = KFrescoVitoDrawable(fixActualImageBounds = true)
    val bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
    drawable.actualImageLayer.canvasTransformationHandler.canvasTransformation =
        object : CanvasTransformation {
          override fun calculateTransformation(
              outTransform: Matrix,
              parentBounds: Rect,
              childWidth: Int,
              childHeight: Int,
          ): Matrix {
            outTransform.setScale(2f, 2f)
            outTransform.postTranslate(10f, 20f)
            return outTransform
          }
        }
    drawable.actualImageLayer.configure(
        dataModel = BitmapImageDataModel(bitmap),
        bounds = Rect(0, 0, 200, 100),
    )
    val outBounds = RectF()

    drawable.getActualImageBounds(outBounds)

    assertThat(outBounds).isEqualTo(RectF(10f, 20f, 210f, 120f))
  }
}
