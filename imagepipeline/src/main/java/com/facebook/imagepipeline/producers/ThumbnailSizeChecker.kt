/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imageutils.BitmapUtil

/**
 * Utility class to consistently check whether a given thumbnail size will be sufficient for a given
 * request with [com.facebook.imagepipeline.common.ResizeOptions].
 */
object ThumbnailSizeChecker {

  /**
   * The ratio between the requested size and the minimum thumbnail size which will be considered
   * big enough. This will allow a thumbnail which is actually 75% of the requested size to be used
   * and scaled up.
   */
  const val ACCEPTABLE_REQUESTED_TO_ACTUAL_SIZE_RATIO = 4.0f / 3
  private const val ROTATED_90_DEGREES_CLOCKWISE = 90
  private const val ROTATED_90_DEGREES_COUNTER_CLOCKWISE = 270

  /**
   * Checks whether the producer may be able to produce images of the specified size. This makes no
   * promise about being able to produce images for a particular source, only generally being able
   * to produce output of the desired resolution.
   *
   * @param width the desired width
   * @param height the desired height
   * @return true if the producer can meet these needs
   */
  @JvmStatic
  fun isImageBigEnough(width: Int, height: Int, resizeOptions: ResizeOptions?): Boolean =
      if (resizeOptions == null) {
        (getAcceptableSize(width) >= BitmapUtil.MAX_BITMAP_DIMENSION &&
            getAcceptableSize(height) >= BitmapUtil.MAX_BITMAP_DIMENSION.toInt())
      } else {
        (getAcceptableSize(width) >= resizeOptions.width &&
            getAcceptableSize(height) >= resizeOptions.height)
      }

  @JvmStatic
  fun isImageBigEnough(encodedImage: EncodedImage?, resizeOptions: ResizeOptions?): Boolean =
      if (encodedImage == null) {
        false
      } else {
        when (encodedImage.rotationAngle) {
          ROTATED_90_DEGREES_CLOCKWISE,
          ROTATED_90_DEGREES_COUNTER_CLOCKWISE -> // Swap width and height when checking size as
              // this will be rotated
              isImageBigEnough(encodedImage.height, encodedImage.width, resizeOptions)
          else -> isImageBigEnough(encodedImage.width, encodedImage.height, resizeOptions)
        }
      }

  @JvmStatic
  fun getAcceptableSize(size: Int): Int = (size * ACCEPTABLE_REQUESTED_TO_ACTUAL_SIZE_RATIO).toInt()
}
