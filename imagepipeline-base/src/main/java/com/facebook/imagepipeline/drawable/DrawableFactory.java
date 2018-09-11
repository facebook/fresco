/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.drawable;

import android.graphics.drawable.Drawable;
import com.facebook.imagepipeline.image.CloseableImage;
import javax.annotation.Nullable;

/**
 * Drawable factory to create Drawables for given images.
 */
public interface DrawableFactory {

  /**
   * Returns true if the factory can create a Drawable for the given image.
   *
   * @param image the image to check
   * @return true if a Drawable can be created
   */
  boolean supportsImageType(CloseableImage image);

  /**
   * Create a drawable for the given image.
   * It is guaranteed that this method is only called if
   * {@link #supportsImageType(CloseableImage)} returned true.
   *
   * @param image the image to create the drawable for
   * @return the Drawable for the image or null if an error occurred
   */
  @Nullable
  Drawable createDrawable(CloseableImage image);
}
