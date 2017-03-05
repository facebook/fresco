/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
