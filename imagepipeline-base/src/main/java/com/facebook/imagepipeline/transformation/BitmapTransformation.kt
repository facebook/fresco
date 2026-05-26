/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transformation

import android.graphics.Bitmap
import com.facebook.imagepipeline.image.QualityInfo

/**
 * In-place bitmap transformation. This interface is similar to Postprocessors, however, it only
 * allows in-place bitmap transformations that are applied immediately after the bitmap has been
 * decoded.
 *
 * NOTE: The original bitmap will not be copied and only the transformed bitmap will be cached in
 * the bitmap memory cache. If the same image is requested without the transformation, it will be
 * decoded again.
 */
interface BitmapTransformation {

  /**
   * Perform an in-place bitmap transformation.
   *
   * @param bitmap the bitmap to transform
   */
  fun transform(bitmap: Bitmap)

  /**
   * Perform an in-place bitmap transformation with quality context. Override this to vary the
   * transformation based on progressive scan number or quality level (e.g., graduated blur).
   *
   * The default implementation delegates to [transform] ignoring quality info.
   *
   * @param bitmap the bitmap to transform
   * @param qualityInfo quality/scan info. For progressive JPEGs, [QualityInfo.getQuality] returns
   *   the scan number when used with a [com.facebook.imagepipeline.decoder.ProgressiveJpegConfig].
   */
  fun transform(bitmap: Bitmap, qualityInfo: QualityInfo) {
    transform(bitmap)
  }

  /**
   * Specify whether the transformation modifies alpha support (transparent images).
   *
   * @return true if the alpha channel is needed
   */
  fun modifiesTransparency(): Boolean
}
