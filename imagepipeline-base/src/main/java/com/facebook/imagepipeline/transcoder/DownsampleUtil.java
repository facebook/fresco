/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import javax.annotation.Nullable;

public class DownsampleUtil {
  public static final int DEFAULT_SAMPLE_SIZE = 1;
  private static final float INTERVAL_ROUNDING = 1.0f/3;

  private DownsampleUtil() {}

  /**
   * Get the factor between the dimensions of the encodedImage (actual image) and the ones of the
   * imageRequest (requested size).
   *
   * @param rotationOptions the rotations options of the request
   * @param resizeOptions the resize options of the request
   * @param encodedImage the encoded image with the actual dimensions
   * @param maxBitmapSize the maximum supported bitmap size (in pixels) when not specified in the
   *     encoded image resizeOptions.
   * @return
   */
  public static int determineSampleSize(
      final RotationOptions rotationOptions,
      @Nullable final ResizeOptions resizeOptions,
      final EncodedImage encodedImage,
      final int maxBitmapSize) {
    if (!EncodedImage.isMetaDataAvailable(encodedImage)) {
      return DEFAULT_SAMPLE_SIZE;
    }
    float ratio = determineDownsampleRatio(rotationOptions, resizeOptions, encodedImage);
    int sampleSize;
    if (encodedImage.getImageFormat() == DefaultImageFormats.JPEG) {
      sampleSize = ratioToSampleSizeJPEG(ratio);
    } else {
      sampleSize = ratioToSampleSize(ratio);
    }

    // Check the case when the dimension of the downsampled image is still larger than the max
    // possible dimension for an image.
    int maxDimension = Math.max(encodedImage.getHeight(), encodedImage.getWidth());
    final float computedMaxBitmapSize =
        resizeOptions != null ? resizeOptions.maxBitmapSize : maxBitmapSize;
    while (maxDimension / sampleSize > computedMaxBitmapSize) {
      if (encodedImage.getImageFormat() == DefaultImageFormats.JPEG) {
        sampleSize *= 2;
      } else {
        sampleSize++;
      }
    }
    return sampleSize;
  }

  @VisibleForTesting
  public static float determineDownsampleRatio(
      final RotationOptions rotationOptions,
      @Nullable final ResizeOptions resizeOptions,
      final EncodedImage encodedImage) {
    Preconditions.checkArgument(EncodedImage.isMetaDataAvailable(encodedImage));
    if (resizeOptions == null || resizeOptions.height <= 0 || resizeOptions.width <= 0
        || encodedImage.getWidth() == 0 || encodedImage.getHeight() == 0) {
      return 1.0f;
    }

    final int rotationAngle = getRotationAngle(rotationOptions, encodedImage);
    final boolean swapDimensions = rotationAngle == 90 || rotationAngle == 270;
    final int widthAfterRotation = swapDimensions ?
            encodedImage.getHeight() : encodedImage.getWidth();
    final int heightAfterRotation = swapDimensions ?
            encodedImage.getWidth() : encodedImage.getHeight();

    final float widthRatio = ((float) resizeOptions.width) / widthAfterRotation;
    final float heightRatio = ((float) resizeOptions.height) / heightAfterRotation;
    float ratio = Math.max(widthRatio, heightRatio);
    FLog.v(
        "DownsampleUtil",
        "Downsample - Specified size: %dx%d, image size: %dx%d "
            + "ratio: %.1f x %.1f, ratio: %.3f",
        resizeOptions.width,
        resizeOptions.height,
        widthAfterRotation,
        heightAfterRotation,
        widthRatio,
        heightRatio,
        ratio);
    return ratio;
  }

  @VisibleForTesting
  public static int ratioToSampleSize(final float ratio) {
    if (ratio > 0.5f + 0.5f * INTERVAL_ROUNDING) {
      return 1; // should have resized
    }
    int sampleSize = 2;
    while (true) {
      double intervalLength = 1.0 / (Math.pow(sampleSize, 2) - sampleSize);
      double compare = (1.0 / sampleSize) + (intervalLength * INTERVAL_ROUNDING);
      if (compare <= ratio) {
        return sampleSize - 1;
      }
      sampleSize++;
    }
  }

  @VisibleForTesting
  public static int ratioToSampleSizeJPEG(final float ratio) {
    if (ratio > 0.5f + 0.5f * INTERVAL_ROUNDING) {
      return 1; // should have resized
    }
    int sampleSize = 2;
    while (true) {
      double intervalLength = 1.0 / (2 * sampleSize);
      double compare = (1.0 / (2 * sampleSize)) + (intervalLength * INTERVAL_ROUNDING);
      if (compare <= ratio) {
        return sampleSize;
      }
      sampleSize *= 2;
    }
  }

  private static int getRotationAngle(
      final RotationOptions rotationOptions, final EncodedImage encodedImage) {
    if (!rotationOptions.useImageMetadata()) {
      return 0;
    }
    int rotationAngle = encodedImage.getRotationAngle();
    Preconditions.checkArgument(rotationAngle == 0 || rotationAngle == 90
        || rotationAngle == 180 || rotationAngle == 270);
    return rotationAngle;
  }

  @VisibleForTesting
  public static int roundToPowerOfTwo(final int sampleSize) {
    int compare = 1;
    while (true) {
      if (compare >= sampleSize) {
        return compare;
      }
      compare *= 2;
    }
  }
}
