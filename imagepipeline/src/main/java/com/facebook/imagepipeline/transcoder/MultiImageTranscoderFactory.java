/* Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.nativecode.NativeJpegTranscoderFactory;
import com.facebook.imagepipeline.platform.PlatformDecoder;
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
  private final PlatformDecoder mPlatformDecoder;
  private final PlatformBitmapFactory mPlatformBitmapFactory;
  @Nullable private final ImageTranscoderFactory mPrimaryImageTranscoderFactory;

  public MultiImageTranscoderFactory(
      final int maxBitmapSize,
      final boolean useDownSamplingRatio,
      final PlatformDecoder platformDecoder,
      final PlatformBitmapFactory platformBitmapFactory,
      @Nullable final ImageTranscoderFactory primaryImageTranscoderFactory) {
    mMaxBitmapSize = maxBitmapSize;
    mUseDownSamplingRatio = useDownSamplingRatio;
    mPlatformDecoder = platformDecoder;
    mPlatformBitmapFactory = platformBitmapFactory;
    mPrimaryImageTranscoderFactory = primaryImageTranscoderFactory;
  }

  @Override
  public ImageTranscoder createImageTranscoder(ImageFormat imageFormat, boolean isResizingEnabled) {
    ImageTranscoder imageTranscoder = getCustomImageTranscoder(imageFormat, isResizingEnabled);
    if (imageTranscoder == null) {
      imageTranscoder = getNativeImageTranscoder(imageFormat, isResizingEnabled);
    }

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
    return new NativeJpegTranscoderFactory(mMaxBitmapSize, mUseDownSamplingRatio)
        .createImageTranscoder(imageFormat, isResizingEnabled);
  }

  private ImageTranscoder getSimpleImageTranscoder(
      ImageFormat imageFormat, boolean isResizingEnabled) {
    return new SimpleImageTranscoderFactory(
            mMaxBitmapSize, mPlatformDecoder, mPlatformBitmapFactory)
        .createImageTranscoder(imageFormat, isResizingEnabled);
  }
}
