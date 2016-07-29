/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import java.util.concurrent.ScheduledExecutorService;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.LinearInterpolator;

import com.facebook.common.time.MonotonicClock;

/**
 * A {@link Drawable} that renders a animated image. The details of the format are abstracted by the
 * {@link AnimatedDrawableBackend} interface. The drawable can work either as an {@link Animatable}
 * where the client calls start/stop to animate it or it can work as a level-based drawable where
 * the client drives the animation by calling {@link Drawable#setLevel}.
 */

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AnimatedDrawable extends AbstractAnimatedDrawable implements AnimatableDrawable {

  public AnimatedDrawable(
      ScheduledExecutorService scheduledExecutorServiceForUiThread,
      AnimatedDrawableCachingBackend animatedDrawableBackend,
      AnimatedDrawableDiagnostics animatedDrawableDiagnostics,
      MonotonicClock monotonicClock) {
    super(scheduledExecutorServiceForUiThread,
        animatedDrawableBackend,
        animatedDrawableDiagnostics,
        monotonicClock);
  }

  @Override
  public ValueAnimator createValueAnimator(int maxDurationMs) {
    ValueAnimator animator = createValueAnimator();
    int repeatCount = Math.max((maxDurationMs / getAnimatedDrawableBackend().getDurationMs()), 1);
    animator.setRepeatCount(repeatCount);
    return animator;
  }

  @Override
  public ValueAnimator createValueAnimator() {
    int loopCount = getAnimatedDrawableBackend().getLoopCount();
    ValueAnimator animator = new ValueAnimator();
    animator.setIntValues(0, getDuration());
    animator.setDuration(getDuration());
    animator.setRepeatCount(
        loopCount != AnimatedImage.LOOP_COUNT_INFINITE ? loopCount : ValueAnimator.INFINITE);
    animator.setRepeatMode(ValueAnimator.RESTART);
    animator.setInterpolator(new LinearInterpolator());
    animator.addUpdateListener(createAnimatorUpdateListener());
    return animator;
  }

  @Override
  public ValueAnimator.AnimatorUpdateListener createAnimatorUpdateListener() {
    return new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        setLevel((Integer) animation.getAnimatedValue());
      }
    };
  }

}
