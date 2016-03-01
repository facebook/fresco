/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.producers;



import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;

public class DownsampleUtil {

  private static final float MAX_BITMAP_SIZE = 2048f;
  private static final float INTERVAL_ROUNDING = 1.0f/3;
  private static final int DEFAULT_SAMPLE_SIZE = 1;

  private DownsampleUtil() {}

  /**
   * Get the factor between the dimensions of the encodedImage (actual image) and the ones of the
   * imageRequest (requested size).
   *
   * @param imageRequest the request containing the requested dimensions
   * @param encodedImage the encoded image with the actual dimensions
   * @return
   */
  public static int determineSampleSize(ImageRequest imageRequest, EncodedImage encodedImage) {
    if (!EncodedImage.isMetaDataAvailable(encodedImage)) {
      return DEFAULT_SAMPLE_SIZE;
    }
    float ratio = determineDownsampleRatio(imageRequest, encodedImage);
    int sampleSize;
    if (encodedImage.getImageFormat() == ImageFormat.JPEG) {
      sampleSize = ratioToSampleSizeJPEG(ratio);
    } else {
      sampleSize = ratioToSampleSize(ratio);
    }

    // Check the case when the dimension of the downsampled image is still larger than the max
    // possible dimension for an image.
    int maxDimension = Math.max(encodedImage.getHeight(), encodedImage.getWidth());
    while (maxDimension / sampleSize > MAX_BITMAP_SIZE) {
      if (encodedImage.getImageFormat() == ImageFormat.JPEG) {
        sampleSize *= 2;
      } else {
        sampleSize++;
      }
    }
    return sampleSize;
  }

  @VisibleForTesting
  static float determineDownsampleRatio(
      ImageRequest imageRequest, EncodedImage encodedImage) {
    Preconditions.checkArgument(EncodedImage.isMetaDataAvailable(encodedImage));
    final ResizeOptions resizeOptions = imageRequest.getResizeOptions();
    if (resizeOptions == null || resizeOptions.height <= 0 || resizeOptions.width <= 0
        || encodedImage.getWidth() == 0 || encodedImage.getHeight() == 0) {
      return 1.0f;
    }

    final int rotationAngle = getRotationAngle(imageRequest, encodedImage);
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
        "Downsample - Specified size: %dx%d, image size: %dx%d " +
            "ratio: %.1f x %.1f, ratio: %.3f for %s",
        resizeOptions.width,
        resizeOptions.height,
        widthAfterRotation,
        heightAfterRotation,
        widthRatio,
        heightRatio,
        ratio,
        imageRequest.getSourceUri().toString());
    return ratio;
  }

  @VisibleForTesting
  static int ratioToSampleSize(float ratio) {
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
  static int ratioToSampleSizeJPEG(float ratio) {
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

  private static int getRotationAngle(ImageRequest imageRequest, EncodedImage encodedImage) {
    if (!imageRequest.getAutoRotateEnabled()) {
      return 0;
    }
    int rotationAngle = encodedImage.getRotationAngle();
    Preconditions.checkArgument(rotationAngle == 0 || rotationAngle == 90
        || rotationAngle == 180 || rotationAngle == 270);
    return rotationAngle;
  }

  @VisibleForTesting
  static int roundToPowerOfTwo(int sampleSize) {
    int compare = 1;
    while (true) {
      if (compare >= sampleSize) {
        return compare;
      }
      compare *= 2;
    }
  }

}
