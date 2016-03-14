/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.factory;


import android.graphics.drawable.Drawable;

import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Factory for instances of {@link AnimatedDrawable}.
 */
public interface AnimatedDrawableFactory {

  /**
   * Creates an {@link AnimatedDrawable} based on an {@link AnimatedImage}.
   *
   * @param closeableImage the result of the code
   * @return a newly constructed {@link Drawable}
   */
  Drawable create(CloseableImage closeableImage);


}
