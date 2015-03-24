/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.view;

import android.view.View;
import android.view.ViewGroup;

/**
 * A utility class that performs measuring based on the desired aspect ratio.
 */
public class AspectRatioMeasure {

  /**
   * Holder for width and height measure specs.
   */
  public static class Spec {
    public int width;
    public int height;
  }

  /**
   * Updates the given measure spec with respect to the aspect ratio.
   *
   * <p>Note: Measure spec is not changed if the aspect ratio is not greater than zero.
   *
   * <p>Measure spec of the layout dimension (width or height) specified as "0dp" is updated
   * to match the measure spec of the other dimension adjusted by the aspect ratio. Exactly one
   * layout dimension should be specified as "0dp".
   *
   * <p>Padding is taken into account so that the aspect ratio refers to the content without
   * padding: {@code aspectRatio == (viewWidth - widthPadding) / (viewHeight - heightPadding)}
   *
   * <p>Updated measure spec respects the parent's constraints. I.e. measure spec is not changed
   * if the parent has specified mode {@code EXACTLY}, and it doesn't exceed measure size if parent
   * has specified mode {@code AT_MOST}.
   *
   * @param spec in/out measure spec to be updated
   * @param aspectRatio desired aspect ratio
   * @param layoutParams view's layout params
   * @param widthPadding view's left + right padding
   * @param heightPadding view's top + bottom padding
   */
  public static void updateMeasureSpec(
      Spec spec,
      float aspectRatio,
      ViewGroup.LayoutParams layoutParams,
      int widthPadding,
      int heightPadding) {
    if (aspectRatio <= 0) {
      return;
    }
    if (shouldAdjust(layoutParams.height)) {
      int widthSpecSize = View.MeasureSpec.getSize(spec.width);
      int desiredHeight = (int) ((widthSpecSize - widthPadding) / aspectRatio + heightPadding);
      int resolvedHeight = View.resolveSize(desiredHeight, spec.height);
      spec.height = View.MeasureSpec.makeMeasureSpec(resolvedHeight, View.MeasureSpec.EXACTLY);
    } else if (shouldAdjust(layoutParams.width)) {
      int heightSpecSize = View.MeasureSpec.getSize(spec.height);
      int desiredWidth = (int) ((heightSpecSize - heightPadding) * aspectRatio + widthPadding);
      int resolvedWidth = View.resolveSize(desiredWidth, spec.width);
      spec.width = View.MeasureSpec.makeMeasureSpec(resolvedWidth, View.MeasureSpec.EXACTLY);
    }
  }

  private static boolean shouldAdjust(int layoutDimension) {
    // Note: wrap_content is supported for backwards compatibility, but should not be used.
    return layoutDimension == 0 || layoutDimension == ViewGroup.LayoutParams.WRAP_CONTENT;
  }
}
