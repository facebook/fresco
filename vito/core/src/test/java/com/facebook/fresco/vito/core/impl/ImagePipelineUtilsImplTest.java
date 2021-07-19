/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.net.Uri;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.TestNativeLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ImagePipelineUtilsImplTest {

  static {
    TestNativeLoader.init();
  }

  private final Uri URI = Uri.parse("test");
  private final ImageDecodeOptions mRoundingDecodeOptions = mock(ImageDecodeOptions.class);
  private final ImageDecodeOptions mRoundingDecodeOptionsAntiAliased =
      mock(ImageDecodeOptions.class);

  private ImagePipelineUtils mImagePipelineUtils;
  private ImagePipelineUtils mImagePipelineUtilsNoNativeRounding;

  @Before
  public void setup() {
    mImagePipelineUtils =
        new ImagePipelineUtilsImpl(
            new DefaultImageDecodeOptionsProviderImpl(new TestCircularBitmapRounding()));
    mImagePipelineUtilsNoNativeRounding =
        new ImagePipelineUtilsImpl(new DefaultImageDecodeOptionsProviderImpl(null));
  }

  @Test
  public void testBuildImageRequest_whenUriNull_thenReturnNull() {
    ImageRequest imageRequest =
        mImagePipelineUtils.buildImageRequest(null, ImageOptions.defaults());

    assertThat(imageRequest).isNull();
  }

  @Test
  public void testBuildImageRequest_whenUriNotNull_thenReturnRequest() {
    ImageRequest imageRequest = mImagePipelineUtils.buildImageRequest(URI, ImageOptions.defaults());

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
  }

  @Test
  public void testBuildImageRequest_whenNoRoundingOptions_thenDoNotRound() {
    final ImageOptions imageOptions = ImageOptions.create().build();

    ImageRequest imageRequest = mImagePipelineUtils.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    assertThat(imageRequest.getImageDecodeOptions()).isEqualTo(ImageDecodeOptions.defaults());
  }

  @Test
  public void testBuildImageRequest_whenRoundAsCircle_thenApplyRoundingParameters() {
    final ImageOptions imageOptions =
        ImageOptions.create().round(RoundingOptions.asCircle()).build();

    ImageRequest imageRequest = mImagePipelineUtils.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    ImageDecodeOptions imageDecodeOptions = imageRequest.getImageDecodeOptions();
    assertThat(imageDecodeOptions).isEqualTo(mRoundingDecodeOptions);
  }

  @Test
  public void
      testBuildImageRequest_whenRoundAsCircleWithAntiAliasing_thenApplyRoundingParameters() {
    final ImageOptions imageOptions =
        ImageOptions.create().round(RoundingOptions.asCircle(true)).build();

    ImageRequest imageRequest = mImagePipelineUtils.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    ImageDecodeOptions imageDecodeOptions = imageRequest.getImageDecodeOptions();
    assertThat(imageDecodeOptions).isEqualTo(mRoundingDecodeOptionsAntiAliased);
  }

  @Test
  public void testBuildImageRequest_whenRoundAsCircleAndRoundingDisabled_thenDoNothing() {
    final ImageOptions imageOptions =
        ImageOptions.create().round(RoundingOptions.asCircle()).build();

    ImageRequest imageRequest =
        mImagePipelineUtilsNoNativeRounding.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    ImageDecodeOptions imageDecodeOptions = imageRequest.getImageDecodeOptions();
    assertThat(imageDecodeOptions).isEqualTo(ImageDecodeOptions.defaults());
  }

  @Test
  public void
      testBuildImageRequest_whenRoundAsCircleWithAntiAliasingAndRoundingDisabled_thenDoNothing() {
    final ImageOptions imageOptions =
        ImageOptions.create().round(RoundingOptions.asCircle(true)).build();

    ImageRequest imageRequest =
        mImagePipelineUtilsNoNativeRounding.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    ImageDecodeOptions imageDecodeOptions = imageRequest.getImageDecodeOptions();
    assertThat(imageDecodeOptions).isEqualTo(ImageDecodeOptions.defaults());
  }

  @Test
  public void testBuildImageRequest_whenResizingEnabled_thenSetResizeOptions() {
    ResizeOptions resizeOptions = ResizeOptions.forDimensions(123, 234);
    final ImageOptions imageOptions = ImageOptions.create().resize(resizeOptions).build();

    ImageRequest imageRequest = mImagePipelineUtils.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    assertThat(imageRequest.getResizeOptions()).isEqualTo(resizeOptions);
  }

  @Test
  public void testBuildImageRequest_whenRotatingEnabled_thenSetRotateOptions() {
    RotationOptions rotationOptions = RotationOptions.forceRotation(RotationOptions.ROTATE_270);
    final ImageOptions imageOptions = ImageOptions.create().rotate(rotationOptions).build();

    ImageRequest imageRequest = mImagePipelineUtils.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    assertThat(imageRequest.getRotationOptions()).isEqualTo(rotationOptions);
  }

  class TestCircularBitmapRounding implements ImagePipelineUtilsImpl.CircularBitmapRounding {

    @Override
    public ImageDecodeOptions getDecodeOptions(boolean antiAliased) {
      return antiAliased ? mRoundingDecodeOptionsAntiAliased : mRoundingDecodeOptions;
    }
  }
}
