/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/**
 * Holds static drawables used in the sample app.
 *
 * <p>Using static set of drawables allows us to easily determine state of image request by simply
 * looking what kind of drawable is passed to image view.
 */
public class Drawables {
  public static void init(final Resources resources) {
    if (sPlaceholderDrawable == null) {
      sPlaceholderDrawable = resources.getDrawable(R.color.placeholder);
    }
    if (sErrorDrawable == null) {
      sErrorDrawable = resources.getDrawable(R.color.error);
    }
  }

  public static Drawable sPlaceholderDrawable;
  public static Drawable sErrorDrawable;

  private Drawables() {}
}
