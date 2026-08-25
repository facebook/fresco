/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.Color
import android.graphics.Rect
import com.facebook.fresco.vito.renderer.ColorIntImageDataModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for zero-duration `ImageLayerDataModel.fadeOut` layer clearing. */
@RunWith(RobolectricTestRunner::class)
class ImageLayerDataModelFadeOutTest {

  private fun configuredLayer(resetOnZeroDurationFadeOut: Boolean): ImageLayerDataModel =
      ImageLayerDataModel(resetOnZeroDurationFadeOut = resetOnZeroDurationFadeOut).apply {
        configure(dataModel = ColorIntImageDataModel(Color.RED), bounds = Rect(0, 0, 100, 100))
      }

  @Test
  fun testFadeOut_zeroDurationFlagOn_clearsLayer() {
    val layer = configuredLayer(resetOnZeroDurationFadeOut = true)
    assertThat(layer.getDataModel()).isNotNull()

    layer.fadeOut(durationMs = 0, resetLayerWhenInvisible = true)

    assertThat(layer.getDataModel()).isNull()
  }

  @Test
  fun testFadeOut_zeroDurationFlagOff_preservesOldBehavior() {
    val layer = configuredLayer(resetOnZeroDurationFadeOut = false)

    layer.fadeOut(durationMs = 0, resetLayerWhenInvisible = true)

    assertThat(layer.getDataModel()).isNotNull()
    assertThat(layer.getAlpha()).isEqualTo(0)
  }

  @Test
  fun testFadeOut_zeroDurationWithoutResetRequest_keepsLayer() {
    val layer = configuredLayer(resetOnZeroDurationFadeOut = true)

    layer.fadeOut(durationMs = 0, resetLayerWhenInvisible = false)

    assertThat(layer.getDataModel()).isNotNull()
    assertThat(layer.getAlpha()).isEqualTo(0)
  }

  @Test
  fun testFadeOut_nonZeroDurationFlagOn_defersToAnimator() {
    val layer = configuredLayer(resetOnZeroDurationFadeOut = true)

    layer.fadeOut(durationMs = 100, resetLayerWhenInvisible = true)

    assertThat(layer.getDataModel()).isNotNull()
  }
}
