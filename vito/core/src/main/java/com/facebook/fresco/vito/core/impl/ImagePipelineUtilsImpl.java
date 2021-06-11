/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.net.Uri;
import com.facebook.fresco.vito.core.ImagePipelineUtils;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Utility methods to create {@link ImageRequest}s for {@link
 * com.facebook.fresco.vito.options.ImageOptions}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePipelineUtilsImpl implements ImagePipelineUtils {

  public interface CircularBitmapRounding {

    ImageDecodeOptions getDecodeOptions(boolean antiAliased);
  }

  public interface ImageDecodeOptionsProvider {

    @Nullable
    ImageDecodeOptions create(
        ImageRequestBuilder imageRequestBuilder, DecodedImageOptions imageOptions);
  }

  private final ImageDecodeOptionsProvider mImageDecodeOptionsProvider;

  public ImagePipelineUtilsImpl(ImageDecodeOptionsProvider imageDecodeOptionsProvider) {
    mImageDecodeOptionsProvider = imageDecodeOptionsProvider;
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

    ResizeOptions resizeOptions = imageOptions.getResizeOptions();
    if (resizeOptions != null) {
      imageRequestBuilder.setResizeOptions(resizeOptions);
    }

    RotationOptions rotationOptions = imageOptions.getRotationOptions();
    if (rotationOptions != null) {
      imageRequestBuilder.setRotationOptions(rotationOptions);
    }

    ImageDecodeOptions imageDecodeOptions =
        mImageDecodeOptionsProvider.create(imageRequestBuilder, imageOptions);
    if (imageDecodeOptions != null) {
      imageRequestBuilder.setImageDecodeOptions(imageDecodeOptions);
    }

    imageRequestBuilder.setLocalThumbnailPreviewsEnabled(
        imageOptions.areLocalThumbnailPreviewsEnabled());

    Postprocessor postprocessor = imageOptions.getPostprocessor();
    if (postprocessor != null) {
      imageRequestBuilder.setPostprocessor(postprocessor);
    }

    if (imageOptions.isProgressiveDecodingEnabled() != null) {
      imageRequestBuilder.setProgressiveRenderingEnabled(
          imageOptions.isProgressiveDecodingEnabled());
    }

    return imageRequestBuilder;
  }

  @Nullable
  protected ImageRequestBuilder createEncodedImageRequestBuilder(
      @Nullable Uri uri, EncodedImageOptions imageOptions) {
    if (uri == null) {
      return null;
    }
    ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
    maybeSetRequestPriority(builder, imageOptions.getPriority());
    return builder;
  }

  @Nullable
  protected ImageRequestBuilder createEncodedImageRequestBuilder(
      ImageRequest imageRequest, EncodedImageOptions imageOptions) {
    if (imageRequest == null) {
      return null;
    }
    ImageRequestBuilder builder = ImageRequestBuilder.fromRequest(imageRequest);
    maybeSetRequestPriority(builder, imageOptions.getPriority());
    return builder;
  }

  private static void maybeSetRequestPriority(
      ImageRequestBuilder builder, @Nullable Priority priority) {
    if (priority != null) {
      builder.setRequestPriority(priority);
    }
  }
}
