/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.net.Uri
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.impl.ImagePipelineUtilsImpl.CircularBitmapRounding
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.core.DownsampleMode
import com.facebook.imagepipeline.testing.TestNativeLoader
import org.assertj.core.api.Java6Assertions
import org.assertj.core.api.Java6Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImagePipelineUtilsImplTest {

  private val URI = Uri.parse("test")
  private val roundingDecodeOptions = mock<ImageDecodeOptions>()
  private val roundingDecodeOptionsAntiAliased = mock<ImageDecodeOptions>()
  private lateinit var imagePipelineUtils: ImagePipelineUtils
  private lateinit var imagePipelineUtilsNoNativeRounding: ImagePipelineUtils

  @Before
  fun setup() {
    imagePipelineUtils =
        ImagePipelineUtilsImpl(DefaultImageDecodeOptionsProviderImpl(TestCircularBitmapRounding()))
    imagePipelineUtilsNoNativeRounding =
        ImagePipelineUtilsImpl(DefaultImageDecodeOptionsProviderImpl(null))
  }

  @Test
  fun testBuildImageRequest_whenUriNull_thenReturnNull() {
    val imageRequest = imagePipelineUtils.buildImageRequest(null, ImageOptions.defaults())
    Java6Assertions.assertThat(imageRequest).isNull()
  }

  @Test
  fun testBuildImageRequest_whenUriNotNull_thenReturnRequest() {
    val imageRequest = imagePipelineUtils.buildImageRequest(URI, ImageOptions.defaults())
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
  }

  @Test
  fun testBuildImageRequest_whenNoRoundingOptions_thenDoNotRound() {
    val imageOptions = ImageOptions.create().build()
    val imageRequest = imagePipelineUtils.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    Java6Assertions.assertThat(imageRequest.imageDecodeOptions)
        .isEqualTo(ImageDecodeOptions.defaults())
  }

  @Test
  fun testBuildImageRequest_whenRoundAsCircle_thenApplyRoundingParameters() {
    val imageOptions = ImageOptions.create().round(RoundingOptions.asCircle()).build()
    val imageRequest = imagePipelineUtils.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    val imageDecodeOptions = imageRequest.imageDecodeOptions
    Java6Assertions.assertThat(imageDecodeOptions).isEqualTo(roundingDecodeOptions)
  }

  @Test
  fun testBuildImageRequest_whenRoundAsCircleWithAntiAliasing_thenApplyRoundingParameters() {
    val imageOptions = ImageOptions.create().round(RoundingOptions.asCircle(true)).build()
    val imageRequest = imagePipelineUtils.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    val imageDecodeOptions = imageRequest.imageDecodeOptions
    Java6Assertions.assertThat(imageDecodeOptions).isEqualTo(roundingDecodeOptionsAntiAliased)
  }

  @Test
  fun testBuildImageRequest_whenRoundAsCircleAndRoundingDisabled_thenDoNothing() {
    val imageOptions = ImageOptions.create().round(RoundingOptions.asCircle()).build()
    val imageRequest = imagePipelineUtilsNoNativeRounding.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    val imageDecodeOptions = imageRequest.imageDecodeOptions
    Java6Assertions.assertThat(imageDecodeOptions).isEqualTo(ImageDecodeOptions.defaults())
  }

  @Test
  fun testBuildImageRequest_whenRoundAsCircleWithAntiAliasingAndRoundingDisabled_thenDoNothing() {
    val imageOptions = ImageOptions.create().round(RoundingOptions.asCircle(true)).build()
    val imageRequest = imagePipelineUtilsNoNativeRounding.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    val imageDecodeOptions = imageRequest.imageDecodeOptions
    Java6Assertions.assertThat(imageDecodeOptions).isEqualTo(ImageDecodeOptions.defaults())
  }

  @Test
  fun testBuildImageRequest_whenResizingEnabled_thenSetResizeOptions() {
    val resizeOptions = ResizeOptions.forDimensions(123, 234)
    val imageOptions = ImageOptions.create().resize(resizeOptions).build()
    val imageRequest = imagePipelineUtils.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    Java6Assertions.assertThat(imageRequest.resizeOptions).isEqualTo(resizeOptions)
  }

  @Test
  fun testBuildImageRequest_whenResizingOverrideDisabled_thenSetOverrideOption() {
    val resizeOptions = ResizeOptions.forDimensions(123, 234)
    val imageOptions =
        ImageOptions.create().resize(resizeOptions).downsampleOverride(DownsampleMode.NEVER).build()
    val imageRequest = imagePipelineUtils.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    Java6Assertions.assertThat(imageRequest.resizeOptions).isEqualTo(resizeOptions)
    Java6Assertions.assertThat(imageRequest.downsampleOverride).isEqualTo(DownsampleMode.NEVER)
  }

  @Test
  fun testBuildImageRequest_whenRotatingEnabled_thenSetRotateOptions() {
    val rotationOptions = RotationOptions.forceRotation(RotationOptions.ROTATE_270)
    val imageOptions = ImageOptions.create().rotate(rotationOptions).build()
    val imageRequest = imagePipelineUtils.buildImageRequest(URI, imageOptions)
    if (imageRequest == null) {
      fail("not null value expected")
      return
    }

    Java6Assertions.assertThat(imageRequest.sourceUri).isEqualTo(URI)
    Java6Assertions.assertThat(imageRequest.rotationOptions).isEqualTo(rotationOptions)
  }

  internal inner class TestCircularBitmapRounding : CircularBitmapRounding {
    override fun getDecodeOptions(antiAliased: Boolean): ImageDecodeOptions =
        if (antiAliased) roundingDecodeOptionsAntiAliased else roundingDecodeOptions
  }

  companion object {
    init {
      TestNativeLoader.init()
    }
  }
}
