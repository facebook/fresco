/* Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.core.ImageTranscoderType;
import com.facebook.imagepipeline.nativecode.NativeImageTranscoderFactory;
import javax.annotation.Nullable;

/**
 * Class responsible of returning the correct {@link ImageTranscoder} given the {@link ImageFormat}.
 * The custom {@link ImageTranscoder}, if any, will always be used first. If the image format is not
 * supported, the first fallback is NativeJpegTranscoder, otherwise {@link SimpleImageTranscoder} is
 * used.
 */
public class MultiImageTranscoderFactory implements ImageTranscoderFactory {

  private final int mMaxBitmapSize;
  private final boolean mUseDownSamplingRatio;
  @Nullable private final ImageTranscoderFactory mPrimaryImageTranscoderFactory;
  @Nullable @ImageTranscoderType private final Integer mImageTranscoderType;

  public MultiImageTranscoderFactory(
      final int maxBitmapSize,
      final boolean useDownSamplingRatio,
      @Nullable final ImageTranscoderFactory primaryImageTranscoderFactory,
      @Nullable @ImageTranscoderType final Integer imageTranscoderType) {
    mMaxBitmapSize = maxBitmapSize;
    mUseDownSamplingRatio = useDownSamplingRatio;
    mPrimaryImageTranscoderFactory = primaryImageTranscoderFactory;
    mImageTranscoderType = imageTranscoderType;
  }

  @Override
  public ImageTranscoder createImageTranscoder(ImageFormat imageFormat, boolean isResizingEnabled) {
    // Use custom ImageTranscoder, if any
    ImageTranscoder imageTranscoder = getCustomImageTranscoder(imageFormat, isResizingEnabled);
    // Use ImageTranscoder based on type passed, if any
    if (imageTranscoder == null) {
      imageTranscoder = getImageTranscoderWithType(imageFormat, isResizingEnabled);
    }
    // First fallback using native ImageTranscoder
    if (imageTranscoder == null) {
      imageTranscoder = getNativeImageTranscoder(imageFormat, isResizingEnabled);
    }

    // Fallback to SimpleImageTranscoder if the format is not supported by native ImageTranscoder
    return imageTranscoder == null
        ? getSimpleImageTranscoder(imageFormat, isResizingEnabled)
        : imageTranscoder;
  }

  @Nullable
  private ImageTranscoder getCustomImageTranscoder(
      ImageFormat imageFormat, boolean isResizingEnabled) {
    if (mPrimaryImageTranscoderFactory == null) {
      return null;
    }
    return mPrimaryImageTranscoderFactory.createImageTranscoder(imageFormat, isResizingEnabled);
  }

  @Nullable
  private ImageTranscoder getNativeImageTranscoder(
      ImageFormat imageFormat, boolean isResizingEnabled) {
    return NativeImageTranscoderFactory.getNativeImageTranscoderFactory(
            mMaxBitmapSize, mUseDownSamplingRatio)
        .createImageTranscoder(imageFormat, isResizingEnabled);
  }

  private ImageTranscoder getSimpleImageTranscoder(
      ImageFormat imageFormat, boolean isResizingEnabled) {
    return new SimpleImageTranscoderFactory(mMaxBitmapSize)
        .createImageTranscoder(imageFormat, isResizingEnabled);
  }

  @Nullable
  private ImageTranscoder getImageTranscoderWithType(
      ImageFormat imageFormat, boolean isResizingEnabled) {
    if (mImageTranscoderType == null) {
      return null;
    }

    switch (mImageTranscoderType) {
      case ImageTranscoderType.NATIVE_TRANSCODER:
        return getNativeImageTranscoder(imageFormat, isResizingEnabled);
      case ImageTranscoderType.JAVA_TRANSCODER:
        return getSimpleImageTranscoder(imageFormat, isResizingEnabled);
      default:
        throw new IllegalArgumentException("Invalid ImageTranscoderType");
    }
  }
}
