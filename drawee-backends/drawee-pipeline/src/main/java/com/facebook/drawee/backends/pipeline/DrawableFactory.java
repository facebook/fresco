/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.backends.pipeline;

import javax.annotation.Nullable;

import android.graphics.drawable.Drawable;

import com.facebook.imagepipeline.image.CloseableImage;

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
