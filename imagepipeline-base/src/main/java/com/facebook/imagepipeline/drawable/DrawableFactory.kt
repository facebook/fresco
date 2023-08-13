/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.drawable

import android.graphics.drawable.Drawable
import com.facebook.imagepipeline.image.CloseableImage

/** Drawable factory to create Drawables for given images. */
interface DrawableFactory {

  /**
   * Returns true if the factory can create a Drawable for the given image.
   *
   * @param image the image to check
   * @return true if a Drawable can be created
   */
  fun supportsImageType(image: CloseableImage): Boolean

  /**
   * Create a drawable for the given image. It is guaranteed that this method is only called if
   * [supportsImageType(CloseableImage)] returned true.
   *
   * @param image the image to create the drawable for
   * @return the Drawable for the image or null if an error occurred
   */
  fun createDrawable(image: CloseableImage): Drawable?
}
