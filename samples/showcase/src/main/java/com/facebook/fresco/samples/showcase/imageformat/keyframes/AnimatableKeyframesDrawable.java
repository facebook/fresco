/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.keyframes;

import android.graphics.drawable.Animatable;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.keyframes.KeyframesDrawable;

/**
 * Animation of KeyframesDrawables needs to be explicitly started and stopped.
 * AnimatableKeyframesDrawable wraps a KeyframesDrawables and allows Fresco to automatically start
 * and stop the animated as needed (as long as setAutoPlayAnimations(true) is called on the
 * DraweeController associated with the DraweeView used to display the image.
 */
class AnimatableKeyframesDrawable extends ForwardingDrawable implements Animatable {

  private final KeyframesDrawable mDrawable;
  private boolean mAnimating;

  AnimatableKeyframesDrawable(KeyframesDrawable drawable) {
    super(drawable);
    mDrawable = drawable;
  }

  @Override
  public void start() {
    mDrawable.startAnimation();
    mAnimating = true;
  }

  @Override
  public void stop() {
    mDrawable.stopAnimation();
    mAnimating = false;
  }

  @Override
  public boolean isRunning() {
    return mAnimating;
  }
}
