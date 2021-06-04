/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.common.logging.FLog;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultImageDecodeOptionsProviderImpl
    implements ImagePipelineUtilsImpl.ImageDecodeOptionsProvider {

  private static final String TAG = "DefaultImageOptionsProvider";

  private final @Nullable ImagePipelineUtilsImpl.CircularBitmapRounding mCircularBitmapRounding;

  public DefaultImageDecodeOptionsProviderImpl(
      @Nullable ImagePipelineUtilsImpl.CircularBitmapRounding circularBitmapRounding) {
    mCircularBitmapRounding = circularBitmapRounding;
  }

  @Nullable
  @Override
  public ImageDecodeOptions create(
      ImageRequestBuilder imageRequestBuilder, DecodedImageOptions imageOptions) {
    ImageDecodeOptions imageDecodeOptions = maybeCreateFromConfigAndCustomDecoder(imageOptions);
    if (imageDecodeOptions == null) {
      imageDecodeOptions =
          maybeSetupPipelineRounding(
              imageOptions.getRoundingOptions(),
              imageOptions.getBitmapConfig(),
              mCircularBitmapRounding);
    }
    return imageDecodeOptions;
  }

  @Nullable
  public static ImageDecodeOptions maybeCreateFromConfigAndCustomDecoder(
      DecodedImageOptions imageOptions) {
    if (imageOptions.getBitmapConfig() != null) {
      if (imageOptions.getRoundingOptions() != null || imageOptions.getPostprocessor() != null) {
        FLog.wtf(TAG, "Trying to use bitmap config incompatible with rounding.");
      } else {
        return ImageDecodeOptions.newBuilder()
            .setBitmapConfig(imageOptions.getBitmapConfig())
            .setCustomImageDecoder(
                imageOptions.getImageDecodeOptions() != null
                    ? imageOptions.getImageDecodeOptions().customImageDecoder
                    : null)
            .build();
      }
    } else if (imageOptions.getImageDecodeOptions() != null
        && imageOptions.getImageDecodeOptions().customImageDecoder != null) {
      return ImageDecodeOptions.newBuilder()
          .setCustomImageDecoder(imageOptions.getImageDecodeOptions().customImageDecoder)
          .build();
    }

    return null;
  }

  @Nullable
  public static ImageDecodeOptions maybeSetupPipelineRounding(
      @Nullable RoundingOptions roundingOptions,
      @Nullable Bitmap.Config bitmapConfig,
      @Nullable ImagePipelineUtilsImpl.CircularBitmapRounding circularBitmapRounding) {
    if (roundingOptions == null
        || roundingOptions.isForceRoundAtDecode()
        || !roundingOptions.isCircular()
        || circularBitmapRounding == null
        || pipelineRoundingUnsupportedForBitmapConfig(bitmapConfig)) {
      return null;
    }
    return circularBitmapRounding.getDecodeOptions(roundingOptions.isAntiAliased());
  }

  private static boolean pipelineRoundingUnsupportedForBitmapConfig(
      @Nullable Bitmap.Config bitmapConfig) {
    return bitmapConfig == Bitmap.Config.RGB_565
        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && bitmapConfig == Bitmap.Config.HARDWARE);
  }
}
