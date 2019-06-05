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

  /**
   * Create or update a drawable for the given image and the previous drawable.
   * It is guaranteed that this method is only called if
   * {@link #needPreviousDrawable(Drawable, CloseableImage)} returned true.
   *
   * @param previousDrawable the previous drawable
   * @param image the image to create the drawable for
   * @return the Drawable for the image and previous drawable or null if an error occurred
   */
  @Nullable
  Drawable createDrawable(Drawable previousDrawable, CloseableImage image);

  /**
   * Returns true if the factory need previous drawable to create or update a Drawable for the given
   * image.
   *
   * @param previousDrawable the previous drawable
   * @param image the image to check
   * @return true if previous drawable is needed
   */
  boolean needPreviousDrawable(Drawable previousDrawable, CloseableImage image);
}
