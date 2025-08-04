/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import com.facebook.imagepipeline.decoder.ImageDecoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/** Tests [ImageDecodeOptions] */
@RunWith(RobolectricTestRunner::class)
class ImageDecodeOptionsTest {
  private lateinit var imageDecoder: ImageDecoder

  @Before
  fun setup() {
    imageDecoder = mock<ImageDecoder>()
  }

  @Test
  @Throws(Exception::class)
  fun testSetFrom_whenUnchanged_thenEqual() {
    val originalOptions = createSampleDecodeOptions()

    val newOptions = ImageDecodeOptions.newBuilder().setFrom(originalOptions).build()

    assertThat(newOptions).isEqualTo(originalOptions)
  }

  @Test
  @Throws(Exception::class)
  fun testSetFrom_whenBooleanChanged_thenNotEqual() {
    val originalOptions = createSampleDecodeOptions()

    val newOptions =
        ImageDecodeOptions.newBuilder().setFrom(originalOptions).setForceStaticImage(false).build()

    assertThat(newOptions).isNotEqualTo(originalOptions)
  }

  @Test
  @Throws(Exception::class)
  fun testSetFrom_whenObjectChanged_thenNotEqual() {
    val originalOptions = createSampleDecodeOptions()

    val newOptions =
        ImageDecodeOptions.newBuilder().setFrom(originalOptions).setCustomImageDecoder(null).build()

    assertThat(newOptions).isNotEqualTo(originalOptions)
  }

  private fun createSampleDecodeOptions(): ImageDecodeOptions {
    return ImageDecodeOptions.newBuilder()
        .setCustomImageDecoder(imageDecoder)
        .setDecodeAllFrames(true)
        .setDecodePreviewFrame(true)
        .setForceStaticImage(true)
        .setMinDecodeIntervalMs(MIN_DECODE_INTERVAL_MS)
        .setUseLastFrameForPreview(true)
        .build()
  }

  companion object {
    private const val MIN_DECODE_INTERVAL_MS = 123
  }
}
