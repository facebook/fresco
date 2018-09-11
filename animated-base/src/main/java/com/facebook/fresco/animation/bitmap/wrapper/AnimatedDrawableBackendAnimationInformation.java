/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
