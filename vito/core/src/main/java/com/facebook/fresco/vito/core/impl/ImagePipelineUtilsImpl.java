/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import javax.annotation.Nullable;

/**
 * Utility methods to create {@link ImageRequest}s for {@link
 * com.facebook.fresco.vito.options.ImageOptions}.
 */
public class ImagePipelineUtilsImpl implements ImagePipelineUtils {

  public interface CircularBitmapRounding {

    ImageDecodeOptions getDecodeOptions(boolean antiAliased);
  }

  private static final String TAG = "ImagePipelineUtils";

  private final @Nullable CircularBitmapRounding mCircularBitmapRounding;

  public ImagePipelineUtilsImpl(@Nullable CircularBitmapRounding circularBitmapRounding) {
    mCircularBitmapRounding = circularBitmapRounding;
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

    // Configure circular bitmap rounding if available
    if (mCircularBitmapRounding != null) {
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

    Postprocessor postprocessor = imageOptions.getPostprocessor();
    if (postprocessor != null) {
      imageRequestBuilder.setPostprocessor(postprocessor);
    }

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
    if (roundingOptions == null || roundingOptions.isForceRoundAtDecode()) {
      return;
    }
    if (roundingOptions.isCircular() && mCircularBitmapRounding != null) {
      imageRequestBuilder.setImageDecodeOptions(
          mCircularBitmapRounding.getDecodeOptions(roundingOptions.isAntiAliased()));
    }
  }
}
