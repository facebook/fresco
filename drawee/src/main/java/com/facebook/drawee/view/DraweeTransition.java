/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.transition.TransitionValues;
import android.view.ViewGroup;
import com.facebook.drawee.drawable.ScalingUtils;

/**
 * This Transition animates changes of {@link GenericDraweeView} between two ScaleTypes
 *
 * In combination with ChangeBounds, DraweeTransition allows GenericDraweeViews
 * that change size, shape, or {@link ScalingUtils.ScaleType} to animate contents
 * smoothly.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class DraweeTransition extends Transition {

  private static final String PROPNAME_BOUNDS = "draweeTransition:bounds";

  private final ScalingUtils.ScaleType mFromScale;
  private final ScalingUtils.ScaleType mToScale;

  public static TransitionSet createTransitionSet(
      ScalingUtils.ScaleType fromScale,
      ScalingUtils.ScaleType toScale) {
    TransitionSet transitionSet = new TransitionSet();
    transitionSet.addTransition(new ChangeBounds());
    transitionSet.addTransition(new DraweeTransition(fromScale, toScale));
    return transitionSet;
  }

  public DraweeTransition(ScalingUtils.ScaleType fromScale, ScalingUtils.ScaleType toScale) {
    this.mFromScale = fromScale;
    this.mToScale = toScale;
  }

  @Override
  public void captureStartValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
  }

  @Override
  public void captureEndValues(TransitionValues transitionValues) {
    captureValues(transitionValues);
  }

  @Override
  public Animator createAnimator(
      ViewGroup sceneRoot,
      TransitionValues startValues,
      TransitionValues endValues) {
    if (startValues == null || endValues == null) {
      return null;
    }
    Rect startBounds = (Rect) startValues.values.get(PROPNAME_BOUNDS);
    Rect endBounds = (Rect) endValues.values.get(PROPNAME_BOUNDS);
    if (startBounds == null || endBounds == null) {
      return null;
    }
    if (mFromScale == mToScale) {
      return null;
    }
    final GenericDraweeView draweeView = (GenericDraweeView) startValues.view;
    final ScalingUtils.InterpolatingScaleType scaleType =
        new ScalingUtils.InterpolatingScaleType(mFromScale, mToScale, startBounds, endBounds);
    draweeView.getHierarchy().setActualImageScaleType(scaleType);

    ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        float fraction = (float) animation.getAnimatedValue();
        scaleType.setValue(fraction);
      }
    });
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        draweeView.getHierarchy().setActualImageScaleType(mToScale);
      }
    });

    return animator;
  }

  private void captureValues(TransitionValues transitionValues) {
    if (transitionValues.view instanceof GenericDraweeView) {
      transitionValues.values.put(
          PROPNAME_BOUNDS,
          new Rect(0, 0, transitionValues.view.getWidth(), transitionValues.view.getHeight()));
    }
  }
}
