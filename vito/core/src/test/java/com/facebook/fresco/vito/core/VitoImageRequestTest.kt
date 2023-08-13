/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.content.res.Resources
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VitoImageRequestTest {

  @Test
  fun testEquals() {
    val res = mock<Resources>()
    val imageSource = mock<ImageSource>()
    val options = mock<ImageOptions>()
    val a = VitoImageRequest(res, imageSource, options, false, null, null)
    val b = VitoImageRequest(res, imageSource, options, false, null, null)

    assertThat(a).isEqualTo(b)
  }

  @Test
  fun testHashCode() {
    val res = mock<Resources>()
    val imageSource = mock<ImageSource>()
    val options = mock<ImageOptions>()
    val a = VitoImageRequest(res, imageSource, options, false, null, null)
    val b = VitoImageRequest(res, imageSource, options, false, null, null)

    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }
}
