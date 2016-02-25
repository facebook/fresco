/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.factory;


import com.facebook.imagepipeline.animated.base.AnimatedDrawable;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Factory for instances of {@link AnimatedDrawable}.
 */
public interface AnimatedDrawableFactory {

  /**
   * Creates an {@link AnimatedDrawable} based on an {@link AnimatedImage}.
   *
   * @param animatedImageResult the result of the code
   * @return a newly constructed {@link AnimatedDrawable}
   */
  AnimatedDrawable create(AnimatedImageResult animatedImageResult);

  /**
   * Creates an {@link AnimatedDrawable} based on an {@link AnimatedImage}.
   *
   * @param animatedImageResult the result of the code
   * @param options additional options
   * @return a newly constructed {@link AnimatedDrawable}
   */
  AnimatedDrawable create(
      AnimatedImageResult animatedImageResult,
      AnimatedDrawableOptions options);

  /**
   * If the image is a CloseableAnimatedImage this method returns the contained Drawable. If not
   * this returns null
   * @param image The CloseableImage to check
   * @return The Drawable if CloseableAnimatedImage or null if not
   */
  AnimatedImageResult getImageIfCloseableAnimatedImage(CloseableImage image);

}
