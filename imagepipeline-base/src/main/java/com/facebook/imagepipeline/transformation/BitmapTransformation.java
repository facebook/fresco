/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transformation;

import android.graphics.Bitmap;

/**
 * In-place bitmap transformation. This interface is similar to Postprocessors, however, it only
 * allows in-place bitmap transformations that are applied immediately after the bitmap has been
 * decoded.
 *
 * <p>NOTE: The original bitmap will not be copied and only the transformed bitmap will be cached in
 * the bitmap memory cache. If the same image is requested without the transformation, it will be
 * decoded again.
 */
public interface BitmapTransformation {

  /**
   * Perform an in-place bitmap transformation.
   *
   * @param bitmap the bitmap to transform
   */
  void transform(Bitmap bitmap);

  /**
   * Specify whether the transformation modifies alpha support (transparent images).
   *
   * @return true if the alpha channel is needed
   */
  boolean modifiesTransparency();
}
