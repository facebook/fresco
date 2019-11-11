/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.net.Uri;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.fresco.vito.core.debug.DebugOverlayFactory;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.fresco.vito.transformation.CircularBitmapTransformation;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.core.NativeCodeSetup;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.infer.annotation.ThreadSafe;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

@ThreadSafe
public class FrescoContext {
  private static final String TAG = "FrescoContext";
  private static final AtomicLong sIdCounter = new AtomicLong();

  private final @Nullable CallerContextVerifier mCallerContextVerifier;
  private final FrescoExperiments mExperiments;
  private final @Nullable ImageListener mGlobalImageListener;
  private final @Nullable ImageStateListener mGlobalImageStateListener;
  private final Hierarcher mHierarcher;
  private final Executor mUiThreadExecutor;
  private final Executor mLightweightBackgroundThreadExecutor;

  private FrescoController mController;
  private FrescoVitoPrefetcher mPrefetcher;

  private @Nullable ImagePipelineFactory mImagePipelineFactory;
  private @Nullable ImageDecodeOptions mCircularImageDecodeOptions;
  private @Nullable ImageDecodeOptions mCircularImageDecodeOptionsAntiAliased;

  public FrescoContext(
      FrescoController controller,
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      Executor lightweightBackgroundThreadExecutor,
      @Nullable ImageListener globalImageListener,
      @Nullable ImageStateListener globalImageStateListener) {
    mController = controller;
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mGlobalImageStateListener = globalImageStateListener;
  }

  public FrescoContext(
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      Executor lightweightBackgroundThreadExecutor,
      @Nullable ImageListener globalImageListener,
      @Nullable ImageStateListener globalImageStateListener,
      DebugOverlayFactory debugOverlayFactory) {
    mController = new FrescoControllerImpl(this, debugOverlayFactory);
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
    mGlobalImageStateListener = globalImageStateListener;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
  }

  public ImagePipelineFactory getImagePipelineFactory() {
    if (mImagePipelineFactory == null) {
      mImagePipelineFactory = ImagePipelineFactory.getInstance();
    }
    return mImagePipelineFactory;
  }

  public void setImagePipelineFactory(@Nullable ImagePipelineFactory imagePipelineFactory) {
    mImagePipelineFactory = imagePipelineFactory;
  }

  public Hierarcher getHierarcher() {
    return mHierarcher;
  }

  public ImagePipeline getImagePipeline() {
    return getImagePipelineFactory().getImagePipeline();
  }

  public FrescoController getController() {
    return mController;
  }

  public FrescoExperiments getExperiments() {
    return mExperiments;
  }

  @Nullable
  public ImageListener getGlobalImageListener() {
    return mGlobalImageListener;
  }

  public ImageStateListener getGlobalImageStateListener() {
    return mGlobalImageStateListener;
  }

  public FrescoVitoPrefetcher getPrefetcher() {
    if (mPrefetcher == null) {
      mPrefetcher = new FrescoVitoPrefetcher(this);
    }
    return mPrefetcher;
  }

  @Nullable
  public ImageRequest buildImageRequest(@Nullable Uri uri, DecodedImageOptions imageOptions) {
    ImageRequestBuilder builder = createDecodedImageRequestBuilder(uri, imageOptions);
    return builder != null ? builder.build() : null;
  }

  @Nullable
  public ImageRequest buildEncodedImageRequest(
      @Nullable Uri uri, EncodedImageOptions imageOptions) {
    ImageRequestBuilder builder = createEncodedImageRequestBuilder(uri, imageOptions);
    return builder != null ? builder.build() : null;
  }

  public void verifyCallerContext(@Nullable Object callerContext) {
    if (mCallerContextVerifier != null) {
      mCallerContextVerifier.verifyCallerContext(callerContext, false);
    }
  }

  @Nullable
  protected ImageRequestBuilder createDecodedImageRequestBuilder(
      @Nullable Uri uri, DecodedImageOptions imageOptions) {
    if (uri == null) {
      return null;
    }
    final ImageRequestBuilder imageRequestBuilder =
        createEncodedImageRequestBuilder(uri, imageOptions);

    if (mExperiments.useNativeRounding() && NativeCodeSetup.getUseNativeCode()) {
      setupNativeRounding(imageRequestBuilder, imageOptions.getRoundingOptions());
    }

    ResizeOptions resizeOptions = imageOptions.getResizeOptions();
    if (resizeOptions != null) {
      imageRequestBuilder.setResizeOptions(resizeOptions);
    }

    RotationOptions rotationOptions = imageOptions.getRotationOptions();
    if (rotationOptions != null) {
      imageRequestBuilder.setRotationOptions(rotationOptions);
    }

    if (imageOptions.getBitmapConfig() != null) {
      if (imageOptions.getRoundingOptions() != null || imageOptions.getPostprocessor() != null) {
        FLog.wtf(TAG, "Trying to use bitmap config incompatible with rounding.");
      } else {
        imageRequestBuilder.setImageDecodeOptions(
            ImageDecodeOptions.newBuilder()
                .setBitmapConfig(imageOptions.getBitmapConfig())
                .setCustomImageDecoder(
                    imageOptions.getImageDecodeOptions() != null
                        ? imageOptions.getImageDecodeOptions().customImageDecoder
                        : null)
                .build());
      }
    } else if (imageOptions.getImageDecodeOptions() != null
        && imageOptions.getImageDecodeOptions().customImageDecoder != null) {
      imageRequestBuilder.setImageDecodeOptions(
          ImageDecodeOptions.newBuilder()
              .setCustomImageDecoder(imageOptions.getImageDecodeOptions().customImageDecoder)
              .build());
    }

    imageRequestBuilder.setLocalThumbnailPreviewsEnabled(
        imageOptions.areLocalThumbnailPreviewsEnabled());

    imageRequestBuilder.setShouldDecodePrefetches(getExperiments().prefetchToBitmapCache());

    imageRequestBuilder.setPostprocessor(imageOptions.getPostprocessor());

    return imageRequestBuilder;
  }

  @Nullable
  protected ImageRequestBuilder createEncodedImageRequestBuilder(
      @Nullable Uri uri, EncodedImageOptions imageOptions) {
    if (uri == null) {
      return null;
    }
    return ImageRequestBuilder.newBuilderWithSource(uri)
        .setRequestPriority(imageOptions.getPriority());
  }

  @VisibleForTesting
  protected void setupNativeRounding(
      final ImageRequestBuilder imageRequestBuilder, @Nullable RoundingOptions roundingOptions) {
    if (roundingOptions == null) {
      return;
    }
    if (roundingOptions.isCircular()) {
      imageRequestBuilder.setImageDecodeOptions(
          getCircularImageDecodeOptions(roundingOptions.isAntiAliased()));
    }
  }

  private synchronized ImageDecodeOptions getCircularImageDecodeOptions(boolean antiAliased) {
    final boolean useFastNativeRounding = mExperiments.useFastNativeRounding();
    if (antiAliased) {
      if (mCircularImageDecodeOptionsAntiAliased == null) {
        mCircularImageDecodeOptionsAntiAliased =
            ImageDecodeOptions.newBuilder()
                .setBitmapTransformation(
                    new CircularBitmapTransformation(true, useFastNativeRounding))
                .build();
      }
      return mCircularImageDecodeOptionsAntiAliased;
    } else {
      if (mCircularImageDecodeOptions == null) {
        mCircularImageDecodeOptions =
            ImageDecodeOptions.newBuilder()
                .setBitmapTransformation(
                    new CircularBitmapTransformation(false, useFastNativeRounding))
                .build();
      }
      return mCircularImageDecodeOptions;
    }
  }

  public Executor getUiThreadExecutorService() {
    return mUiThreadExecutor;
  }

  public Executor getLightweightBackgroundThreadExecutor() {
    return mLightweightBackgroundThreadExecutor;
  }

  public void setController(FrescoController controller) {
    mController = controller;
  }

  public static long generateIdentifier() {
    return sIdCounter.incrementAndGet();
  }
}
