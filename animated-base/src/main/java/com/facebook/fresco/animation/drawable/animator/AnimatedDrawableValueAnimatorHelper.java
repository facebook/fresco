/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable.animator;

import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Helper class to create {@link ValueAnimator}s for animated drawables. Currently, this class only
 * supports API 11 (Honeycomb) and above.
 *
 * <p>Supported drawable types: - {@link AnimatedDrawable2}
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class AnimatedDrawableValueAnimatorHelper {

  /**
   * Create a value animator for the given animation drawable and max animation duration in ms.
   *
   * @param drawable the drawable to create the animator for
   * @param maxDurationMs the max duration in ms
   * @return the animator to use
   */
  @Nullable
  public static ValueAnimator createValueAnimator(Drawable drawable, int maxDurationMs) {
    if (drawable instanceof AnimatedDrawable2) {
      return AnimatedDrawable2ValueAnimatorHelper.createValueAnimator(
          (AnimatedDrawable2) drawable, maxDurationMs);
    }
    return null;
  }

  /**
   * Create a value animator for the given animation drawable.
   *
   * @param drawable the drawable to create the animator for
   * @return the animator to use
   */
  @Nullable
  public static ValueAnimator createValueAnimator(Drawable drawable) {
    if (drawable instanceof AnimatedDrawable2) {
      final AnimatedDrawable2 animatedDrawable2 = (AnimatedDrawable2) drawable;
      return AnimatedDrawable2ValueAnimatorHelper.createValueAnimator(
          animatedDrawable2,
          animatedDrawable2.getLoopCount(),
          animatedDrawable2.getLoopDurationMs());
    }
    return null;
  }

  /**
   * Create an animator update listener to be used to update the drawable to be animated.
   *
   * @param drawable the drawable to create the animator update listener for
   * @return the listener to use
   */
  @Nullable
  public static ValueAnimator.AnimatorUpdateListener createAnimatorUpdateListener(
      final Drawable drawable) {
    if (drawable instanceof AnimatedDrawable2) {
      return AnimatedDrawable2ValueAnimatorHelper.createAnimatorUpdateListener(
          (AnimatedDrawable2) drawable);
    }
    return null;
  }

  private AnimatedDrawableValueAnimatorHelper() {}
}
