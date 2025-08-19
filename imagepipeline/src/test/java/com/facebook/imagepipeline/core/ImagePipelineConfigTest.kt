/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import android.net.Uri
import com.facebook.imagepipeline.core.ImagePipelineConfig.Companion.defaultImageRequestConfig
import com.facebook.imagepipeline.core.ImagePipelineConfig.Companion.resetDefaultRequestConfig
import com.facebook.imagepipeline.request.ImageRequestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/** Some tests for ImagePipelineConfigTest */
@RunWith(RobolectricTestRunner::class)
class ImagePipelineConfigTest {
  private lateinit var uri: Uri

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    uri = Mockito.mock(Uri::class.java)
  }

  @Test
  fun testDefaultConfigIsFalseByDefault() {
    resetDefaultRequestConfig()
    assertThat(defaultImageRequestConfig.isProgressiveRenderingEnabled).isFalse()
  }

  @Test
  fun testDefaultConfigIsTrueIfChanged() {
    resetDefaultRequestConfig()
    defaultImageRequestConfig.isProgressiveRenderingEnabled = true
    assertThat(defaultImageRequestConfig.isProgressiveRenderingEnabled).isTrue()
  }

  @Test
  fun testImageRequestDefault() {
    resetDefaultRequestConfig()
    val imageRequest = ImageRequestBuilder.newBuilderWithSource(uri).build()
    assertThat(imageRequest.getProgressiveRenderingEnabled()).isFalse()
  }

  @Test
  fun testImageRequestWhenChanged() {
    resetDefaultRequestConfig()
    defaultImageRequestConfig.isProgressiveRenderingEnabled = true
    val imageRequest = ImageRequestBuilder.newBuilderWithSource(uri).build()
    assertThat(imageRequest.getProgressiveRenderingEnabled()).isTrue()
  }

  @Test
  fun testImageRequestWhenChangedAndOverriden() {
    resetDefaultRequestConfig()
    val imageRequest =
        ImageRequestBuilder.newBuilderWithSource(uri).setProgressiveRenderingEnabled(true).build()
    assertThat(imageRequest.getProgressiveRenderingEnabled()).isTrue()
    val imageRequest2 =
        ImageRequestBuilder.newBuilderWithSource(uri).setProgressiveRenderingEnabled(false).build()
    assertThat(imageRequest2.getProgressiveRenderingEnabled()).isFalse()
  }
}
