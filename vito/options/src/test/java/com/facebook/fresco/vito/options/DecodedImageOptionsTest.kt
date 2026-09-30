/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DecodedImageOptionsTest {

  @Test
  fun `single frame rendering defaults to disabled`() {
    assertThat(ImageOptions.defaults().enableSingleFrameRendering).isFalse()
  }

  @Test
  fun `single frame rendering is preserved when extending options`() {
    val options = ImageOptions.create().enableSingleFrameRendering(true).build()

    val extendedOptions = options.extend().build()

    assertThat(extendedOptions.enableSingleFrameRendering).isTrue()
    assertThat(extendedOptions).isEqualTo(options)
    assertThat(extendedOptions.hashCode()).isEqualTo(options.hashCode())
  }

  @Test
  fun `single frame rendering participates in equality`() {
    val disabled = ImageOptions.create().build()
    val enabled = ImageOptions.create().enableSingleFrameRendering(true).build()

    assertThat(enabled).isNotEqualTo(disabled)
    assertThat(enabled.hashCode()).isNotEqualTo(disabled.hashCode())
  }
}
