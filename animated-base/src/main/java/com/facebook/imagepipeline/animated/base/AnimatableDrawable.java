/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import android.graphics.drawable.Animatable;

import com.nineoldandroids.animation.ValueAnimator;

/**
 * An interface for animatable drawables that can be asked to construct a value animator.
 */
public interface AnimatableDrawable extends Animatable {

  /**
   * An animator that will animate the drawable directly. The loop count and duration will
   * be determined by metadata in the original image. Update listener is attached automatically.
   *
   * @return a new animator
   */
  ValueAnimator createValueAnimator();

  /**
   * An animator that will animate the drawable directly. The loop count will be set based on
   * the specified duration. Update listener is attached automatically.
   *
   * @param maxDurationMs maximum duration animate
   * @return a new animator
   */
  ValueAnimator createValueAnimator(int maxDurationMs);

  /**
   * Creates an animator update listener that will animate the drawable directly. This is useful
   * when the drawable needs to be animated by an existing value animator.
   * @return a new update listener
   */
  ValueAnimator.AnimatorUpdateListener createAnimatorUpdateListener();
}
