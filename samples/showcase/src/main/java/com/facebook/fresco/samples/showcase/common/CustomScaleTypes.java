/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.common;

import android.graphics.Matrix;
import android.graphics.Rect;
import com.facebook.drawee.drawable.ScalingUtils;

/** Custom scale type examples. */
public class CustomScaleTypes {

  public static final ScalingUtils.ScaleType FIT_X = new ScaleTypeFitX();
  public static final ScalingUtils.ScaleType FIT_Y = new ScaleTypeFitY();

  private static class ScaleTypeFitX extends ScalingUtils.AbstractScaleType {

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale, dx, dy;
      scale = scaleX;
      dx = parentRect.left;
      dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeFitY extends ScalingUtils.AbstractScaleType {

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale, dx, dy;
      scale = scaleY;
      dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      dy = parentRect.top;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }
}
