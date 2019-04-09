/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.fresco.vito.drawable.VitoDrawableFactory;
import com.facebook.fresco.vito.drawable.VitoDrawableFactoryImpl;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.fresco.vito.transformation.CircularBitmapTransformation;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
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
  private final FrescoController mController;
  private final Hierarcher mHierarcher;
  private final Executor mUiThreadExecutor;

  private @Nullable ImagePipelineFactory mImagePipelineFactory;
  private @Nullable VitoDrawableFactory mDrawableFactory;
  private @Nullable ImageDecodeOptions mCircularImageDecodeOptions;
  private @Nullable ImageDecodeOptions mCircularImageDecodeOptionsAntiAliased;

  public FrescoContext(
      FrescoController controller,
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      @Nullable ImageListener globalImageListener) {
    mController = controller;
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
  }

  public FrescoContext(
      Hierarcher hierarcher,
      @Nullable CallerContextVerifier callerContextVerifier,
      FrescoExperiments frescoExperiments,
      Executor uiThreadExecutor,
      @Nullable ImageListener globalImageListener) {
    mController = new FrescoControllerImpl(this);
    mHierarcher = hierarcher;
    mCallerContextVerifier = callerContextVerifier;
    mExperiments = frescoExperiments;
    mUiThreadExecutor = uiThreadExecutor;
    mGlobalImageListener = globalImageListener;
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

  public synchronized VitoDrawableFactory getDrawableFactory(Resources resources) {
    if (mDrawableFactory == null) {
      mDrawableFactory =
          new VitoDrawableFactoryImpl(
              resources, getImagePipelineFactory().getAnimatedDrawableFactory(null));
    }
    return mDrawableFactory;
  }

  @Nullable
  public ImageListener getGlobalImageListener() {
    return mGlobalImageListener;
  }

  @Nullable
  public ImageRequest buildImageRequest(@Nullable Uri uri, ImageOptions imageOptions) {
    ImageRequestBuilder builder = createImageRequestBuilder(uri, imageOptions);
    return builder != null ? builder.build() : null;
  }

  public void verifyCallerContext(@Nullable Object callerContext) {
    if (mCallerContextVerifier != null) {
      mCallerContextVerifier.verifyCallerContext(callerContext);
    }
  }

  @TargetApi(Build.VERSION_CODES.O)
  @Nullable
  protected ImageRequestBuilder createImageRequestBuilder(
      @Nullable Uri uri, ImageOptions imageOptions) {
    if (uri == null) {
      return null;
    }
    final ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri);

    setupRounding(imageRequestBuilder, imageOptions.getRoundingOptions());

    ResizeOptions resizeOptions = imageOptions.getResizeOptions();
    if (resizeOptions != null) {
      imageRequestBuilder.setResizeOptions(resizeOptions);
    }

    imageRequestBuilder.setRequestPriority(imageOptions.getPriority());

    if (imageOptions.getBitmapConfig() != null) {
      if (imageOptions.getRoundingOptions() != null || imageOptions.getPostprocessor() != null) {
        FLog.wtf(TAG, "Trying to use bitmap config incompatible with rounding.");
      } else {
        imageRequestBuilder.setImageDecodeOptions(
            ImageDecodeOptions.newBuilder()
                .setBitmapConfig(imageOptions.getBitmapConfig())
                .build());
      }
    }

    imageRequestBuilder.setLocalThumbnailPreviewsEnabled(
        imageOptions.areLocalThumbnailPreviewsEnabled());

    imageRequestBuilder.setShouldDecodePrefetches(getExperiments().prefetchToBitmapCache());

    imageRequestBuilder.setPostprocessor(imageOptions.getPostprocessor());

    return imageRequestBuilder;
  }

  @VisibleForTesting
  protected void setupRounding(
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
    if (antiAliased) {
      if (mCircularImageDecodeOptionsAntiAliased == null) {
        mCircularImageDecodeOptionsAntiAliased =
            ImageDecodeOptions.newBuilder()
                .setBitmapTransformation(new CircularBitmapTransformation(true))
                .build();
      }
      return mCircularImageDecodeOptionsAntiAliased;
    } else {
      if (mCircularImageDecodeOptions == null) {
        mCircularImageDecodeOptions =
            ImageDecodeOptions.newBuilder()
                .setBitmapTransformation(new CircularBitmapTransformation(false))
                .build();
      }
      return mCircularImageDecodeOptions;
    }
  }

  public Executor getUiThreadExecutorService() {
    return mUiThreadExecutor;
  }

  public static long generateIdentifier() {
    return sIdCounter.incrementAndGet();
  }
}
