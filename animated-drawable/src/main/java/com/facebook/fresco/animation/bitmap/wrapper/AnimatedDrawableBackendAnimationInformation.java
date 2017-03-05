/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.fresco.animation.bitmap.wrapper;

import com.facebook.fresco.animation.backend.AnimationInformation;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;

/**
 * {@link AnimationInformation} that wraps an {@link AnimatedDrawableBackend}.
 */
public class AnimatedDrawableBackendAnimationInformation
    implements AnimationInformation {

  private final AnimatedDrawableBackend mAnimatedDrawableBackend;

  public AnimatedDrawableBackendAnimationInformation(
      AnimatedDrawableBackend animatedDrawableBackend) {
    mAnimatedDrawableBackend = animatedDrawableBackend;
  }

  @Override
  public int getFrameCount() {
    return mAnimatedDrawableBackend.getFrameCount();
  }

  @Override
  public int getFrameDurationMs(int frameNumber) {
    return mAnimatedDrawableBackend.getDurationMsForFrame(frameNumber);
  }

  @Override
  public int getLoopCount() {
    return mAnimatedDrawableBackend.getLoopCount();
  }
}
