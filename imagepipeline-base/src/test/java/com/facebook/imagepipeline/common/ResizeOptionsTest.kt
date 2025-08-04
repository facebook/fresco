/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import com.facebook.imagepipeline.common.ResizeOptions.Companion.forDimensions
import com.facebook.imagepipeline.common.ResizeOptions.Companion.forSquareSize
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResizeOptionsTest {
  @Test
  fun testStaticConstructorWithValidWidthAndHeight() {
    val resizeOptions = forDimensions(1, 2)

    assertThat(resizeOptions).isNotNull()
    assertThat(resizeOptions?.width).isEqualTo(1)
    assertThat(resizeOptions?.height).isEqualTo(2)
  }

  @Test
  fun testStaticConstructorWithInvalidWidth() {
    val resizeOptions = forDimensions(0, 2)

    assertThat(resizeOptions).isNull()
  }

  @Test
  fun testStaticConstructorWithInvalidHeight() {
    val resizeOptions = forDimensions(1, 0)

    assertThat(resizeOptions).isNull()
  }

  @Test
  fun testStaticConstructorWithValidSquareSize() {
    val resizeOptions = forSquareSize(1)

    assertThat(resizeOptions).isNotNull()
    assertThat(resizeOptions?.width).isEqualTo(1)
    assertThat(resizeOptions?.height).isEqualTo(1)
  }

  @Test
  fun testStaticConstructorWithInvalidSquareSize() {
    val resizeOptions = forSquareSize(0)

    assertThat(resizeOptions).isNull()
  }
}
