/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import android.graphics.Rect;

import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;

/**
 * Assisted provider for {@link AnimatedDrawableBackend}.
 */
public interface AnimatedDrawableBackendProvider {

  /**
   * Creates a new {@link AnimatedDrawableBackend}.
   *
   * @param animatedImageResult the image result.
   * @param bounds the initial bounds for the drawable
   * @return a new {@link AnimatedDrawableBackend}
   */
  AnimatedDrawableBackend get(AnimatedImageResult animatedImageResult, Rect bounds);
}
