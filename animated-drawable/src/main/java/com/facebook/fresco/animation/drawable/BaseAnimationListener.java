/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.drawable;

/**
 * Base animation listener. This convenience class can be used to simplify the code if the extending
 * class is not interested in all events. Just override the ones you need.
 *
 * See {@link AnimationListener} for more information.
 */
public class BaseAnimationListener implements AnimationListener {

  @Override
  public void onAnimationStart(AnimatedDrawable2 drawable) {
  }

  @Override
  public void onAnimationStop(AnimatedDrawable2 drawable) {
  }

  @Override
  public void onAnimationReset(AnimatedDrawable2 drawable) {
  }

  @Override
  public void onAnimationRepeat(AnimatedDrawable2 drawable) {
  }

  @Override
  public void onAnimationFrame(AnimatedDrawable2 drawable, int frameNumber) {
  }
}
