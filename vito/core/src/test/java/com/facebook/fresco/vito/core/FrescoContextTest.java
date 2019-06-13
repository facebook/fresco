/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.net.Uri;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.fresco.vito.core.debug.NoOpDebugOverlayFactory;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.fresco.vito.transformation.CircularBitmapTransformation;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FrescoContextTest {

  private static final Uri URI = Uri.parse("test");

  private Hierarcher mHierarcher;
  private CallerContextVerifier mCallerContextVerifier;
  private FrescoExperiments mFrescoExperiments;
  private ImageListener mImageListener;
  private Executor mUiThreadExecutor;

  private FrescoContext mFrescoContext;

  @Before
  public void setup() {
    mHierarcher = mock(Hierarcher.class);
    mCallerContextVerifier = mock(CallerContextVerifier.class);
    mFrescoExperiments = mock(FrescoExperiments.class);
    mImageListener = mock(ImageListener.class);
    mUiThreadExecutor = mock(Executor.class);

    mFrescoContext =
        new FrescoContext(
            mHierarcher,
            mCallerContextVerifier,
            mFrescoExperiments,
            mUiThreadExecutor,
            mImageListener,
            new NoOpDebugOverlayFactory());
  }

  @Test
  public void testBuildImageRequest_whenUriNull_thenReturnNull() {
    ImageRequest imageRequest = mFrescoContext.buildImageRequest(null, ImageOptions.defaults());

    assertThat(imageRequest).isNull();
  }

  @Test
  public void testBuildImageRequest_whenUriNotNull_thenReturnRequest() {
    ImageRequest imageRequest = mFrescoContext.buildImageRequest(URI, ImageOptions.defaults());

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
  }

  @Test
  public void testBuildImageRequest_whenNoRoundingOptions_thenDoNotRound() {
    final ImageOptions imageOptions = ImageOptions.create().build();

    ImageRequest imageRequest = mFrescoContext.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    assertThat(imageRequest.getImageDecodeOptions()).isEqualTo(ImageDecodeOptions.defaults());
  }

  @Test
  public void testBuildImageRequest_whenRoundAsCircle_thenApplyRoundingParameters() {
    final ImageOptions imageOptions =
        ImageOptions.create().round(RoundingOptions.asCircle()).build();

    ImageRequest imageRequest = mFrescoContext.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    ImageDecodeOptions imageDecodeOptions = imageRequest.getImageDecodeOptions();
    assertThat(imageDecodeOptions).isNotEqualTo(ImageDecodeOptions.defaults());
    assertThat(imageDecodeOptions.bitmapTransformation)
        .isInstanceOf(CircularBitmapTransformation.class);

    CircularBitmapTransformation transformation =
        (CircularBitmapTransformation) imageDecodeOptions.bitmapTransformation;
    assertThat(transformation).isNotNull();
    assertThat(transformation.isAntiAliased()).isFalse();
  }

  @Test
  public void
      testBuildImageRequest_whenRoundAsCircleWithAntiAliasing_thenApplyRoundingParameters() {
    final ImageOptions imageOptions =
        ImageOptions.create().round(RoundingOptions.asCircle(true)).build();

    ImageRequest imageRequest = mFrescoContext.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    ImageDecodeOptions imageDecodeOptions = imageRequest.getImageDecodeOptions();
    assertThat(imageDecodeOptions).isNotEqualTo(ImageDecodeOptions.defaults());
    assertThat(imageDecodeOptions.bitmapTransformation)
        .isInstanceOf(CircularBitmapTransformation.class);

    CircularBitmapTransformation transformation =
        (CircularBitmapTransformation) imageDecodeOptions.bitmapTransformation;
    assertThat(transformation).isNotNull();
    assertThat(transformation.isAntiAliased()).isTrue();
  }

  @Test
  public void testBuildImageRequest_whenResizingEnabled_thenSetResizeOptions() {
    ResizeOptions resizeOptions = ResizeOptions.forDimensions(123, 234);
    final ImageOptions imageOptions = ImageOptions.create().resize(resizeOptions).build();

    ImageRequest imageRequest = mFrescoContext.buildImageRequest(URI, imageOptions);

    assertThat(imageRequest).isNotNull();
    assertThat(imageRequest.getSourceUri()).isEqualTo(URI);
    assertThat(imageRequest.getResizeOptions()).isEqualTo(resizeOptions);
  }
}
