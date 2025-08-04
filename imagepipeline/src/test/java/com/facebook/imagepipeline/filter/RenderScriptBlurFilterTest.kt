/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.filter

import android.content.Context
import android.graphics.Bitmap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RenderScriptBlurFilterTest {
  @Mock private lateinit var srcBitmap: Bitmap
  @Mock private lateinit var destBitmap: Bitmap
  @Mock private lateinit var context: Context

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
  }

  @Test(expected = IllegalArgumentException::class)
  fun invalidNegativeRadiusBlur() {
    RenderScriptBlurFilter.blurBitmap(destBitmap, srcBitmap, context, -1)
  }

  @Test(expected = IllegalArgumentException::class)
  fun invalidZeroRadiusBlur() {
    RenderScriptBlurFilter.blurBitmap(destBitmap, srcBitmap, context, 0)
  }

  @Test(expected = IllegalArgumentException::class)
  fun invalidBigRadiusBlur() {
    RenderScriptBlurFilter.blurBitmap(
        destBitmap, srcBitmap, context, RenderScriptBlurFilter.BLUR_MAX_RADIUS + 1)
  }
}
