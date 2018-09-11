/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imageutils.BitmapUtil;

/**
 * Utility class to consistently check whether a given thumbnail size will be sufficient for a given
 * request with {@link com.facebook.imagepipeline.common.ResizeOptions}.
 */
public final class ThumbnailSizeChecker {

  /**
   * The ratio between the requested size and the minimum thumbnail size which will be considered
   * big enough. This will allow a thumbnail which is actually 75% of the requested size to be used
   * and scaled up.
   */
  public static final float ACCEPTABLE_REQUESTED_TO_ACTUAL_SIZE_RATIO = 4.0f/3;

  private static final int ROTATED_90_DEGREES_CLOCKWISE = 90;
  private static final int ROTATED_90_DEGREES_COUNTER_CLOCKWISE = 270;

  /**
   * Checks whether the producer may be able to produce images of the specified size. This makes no
   * promise about being able to produce images for a particular source, only generally being able
   * to produce output of the desired resolution.
   *
   * @param width the desired width
   * @param height the desired height
   * @return true if the producer can meet these needs
   */
  public static boolean isImageBigEnough(int width, int height, ResizeOptions resizeOptions) {
    if (resizeOptions == null) {
      return getAcceptableSize(width) >= BitmapUtil.MAX_BITMAP_SIZE
          && getAcceptableSize(height) >= (int) BitmapUtil.MAX_BITMAP_SIZE;
    } else {
      return getAcceptableSize(width) >= resizeOptions.width
          && getAcceptableSize(height) >= resizeOptions.height;
    }
  }

  public static boolean isImageBigEnough(EncodedImage encodedImage, ResizeOptions resizeOptions) {
    if (encodedImage == null) {
      return false;
    }

    switch (encodedImage.getRotationAngle()) {
      case ROTATED_90_DEGREES_CLOCKWISE:
      case ROTATED_90_DEGREES_COUNTER_CLOCKWISE:
        // Swap width and height when checking size as this will be rotated
        return isImageBigEnough(encodedImage.getHeight(), encodedImage.getWidth(), resizeOptions);
      default:
        return isImageBigEnough(encodedImage.getWidth(), encodedImage.getHeight(), resizeOptions);
    }
  }

  public static int getAcceptableSize(int size) {
    return (int) (size * ACCEPTABLE_REQUESTED_TO_ACTUAL_SIZE_RATIO);
  }
}
