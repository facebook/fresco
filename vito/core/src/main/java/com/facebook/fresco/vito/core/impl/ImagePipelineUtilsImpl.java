/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.fresco.vito.transformation.CircularBitmapTransformation;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.NativeCodeSetup;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import javax.annotation.Nullable;

/**
 * Utility methods to create {@link ImageRequest}s for {@link
 * com.facebook.fresco.vito.options.ImageOptions}.
 */
public class ImagePipelineUtilsImpl implements ImagePipelineUtils {

  private static final String TAG = "ImagePipelineUtils";

  private final FrescoExperiments mExperiments;

  private @Nullable ImageDecodeOptions mCircularImageDecodeOptions;
  private @Nullable ImageDecodeOptions mCircularImageDecodeOptionsAntiAliased;

  public ImagePipelineUtilsImpl(FrescoExperiments frescoExperiments) {
    mExperiments = frescoExperiments;
  }

  @Override
  @Nullable
  public ImageRequest buildImageRequest(@Nullable Uri uri, DecodedImageOptions imageOptions) {
    if (uri == null) {
      return null;
    }
    final ImageRequestBuilder imageRequestBuilder =
        createEncodedImageRequestBuilder(uri, imageOptions);
    ImageRequestBuilder builder =
        createDecodedImageRequestBuilder(imageRequestBuilder, imageOptions);
    return builder != null ? builder.build() : null;
  }

  @Override
  @Nullable
  public ImageRequest wrapDecodedImageRequest(
      ImageRequest imageRequest, DecodedImageOptions imageOptions) {
    ImageRequestBuilder builder =
        createDecodedImageRequestBuilder(
            createEncodedImageRequestBuilder(imageRequest, imageOptions), imageOptions);
    return builder != null ? builder.build() : null;
  }

  @Override
  @Nullable
  public ImageRequest buildEncodedImageRequest(
      @Nullable Uri uri, EncodedImageOptions imageOptions) {
    ImageRequestBuilder builder = createEncodedImageRequestBuilder(uri, imageOptions);
    return builder != null ? builder.build() : null;
  }

  @Nullable
  protected ImageRequestBuilder createDecodedImageRequestBuilder(
      @Nullable ImageRequestBuilder imageRequestBuilder, DecodedImageOptions imageOptions) {
    if (imageRequestBuilder == null) {
      return null;
    }

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

    imageRequestBuilder.setShouldDecodePrefetches(mExperiments.prefetchToBitmapCache());

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

  @Nullable
  protected ImageRequestBuilder createEncodedImageRequestBuilder(
      ImageRequest imageRequest, EncodedImageOptions imageOptions) {
    if (imageRequest == null) {
      return null;
    }
    return ImageRequestBuilder.fromRequest(imageRequest)
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
}
