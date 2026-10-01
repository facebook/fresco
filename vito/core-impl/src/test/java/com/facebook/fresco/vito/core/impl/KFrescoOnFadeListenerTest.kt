/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.fresco.ui.common.OnFadeListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for `onFadeListener` storage and `ImageLayerDataModel.fadeIn` end callbacks. */
@RunWith(RobolectricTestRunner::class)
class KFrescoOnFadeListenerTest {

  private val fakeFadeListener =
      object : OnFadeListener {
        var started = 0
        var finished = 0
        var shownImmediately = 0

        override fun onFadeStarted() {
          started++
        }

        override fun onFadeFinished() {
          finished++
        }

        override fun onShownImmediately() {
          shownImmediately++
        }
      }

  @Test
  fun testReset_clearsOnFadeListener() {
    val drawable = KFrescoVitoDrawable()
    drawable.onFadeListener = fakeFadeListener

    drawable.reset()

    assertThat(drawable.onFadeListener).isNull()
  }

  @Test
  fun testFadeIn_endCallback_firesWhenAnimatorEnds() {
    val layer = ImageLayerDataModel()
    var calls = 0

    layer.fadeIn(durationMs = 100) { calls++ }
    layer.reset()

    assertThat(calls).isEqualTo(1)
  }

  @Test
  fun testFadeIn_zeroDuration_doesNotFireEndCallback() {
    val layer = ImageLayerDataModel()
    var calls = 0

    layer.fadeIn(durationMs = 0) { calls++ }

    assertThat(calls).isEqualTo(0)
    assertThat(layer.getAlpha()).isEqualTo(255)
  }
}
