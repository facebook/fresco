/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/**
 * Set of properties for drawable. There are no default values and only gets applied if were set
 * explicitly.
 */
public class DrawableProperties {

  private final Property<Integer> mAlphaProperty = new Property<>();
  private final Property<ColorFilter> mColorFilterProperty = new Property<>();
  private final Property<Boolean> mDitherProperty = new Property<>();
  private final Property<Boolean> mFilterBitmapProperty = new Property<>();

  public void setAlpha(int alpha) {
    mAlphaProperty.setValue(alpha);
  }

  public void setColorFilter(ColorFilter colorFilter) {
    mColorFilterProperty.setValue(colorFilter);
  }

  public void setDither(boolean dither) {
    mDitherProperty.setValue(dither);
  }

  public void setFilterBitmap(boolean filterBitmap) {
    mFilterBitmapProperty.setValue(filterBitmap);
  }

  public void applyTo(Drawable drawable) {
    if (drawable == null) {
      return;
    }
    if (mAlphaProperty.mIsSet) {
      drawable.setAlpha(mAlphaProperty.mValue);
    }
    if (mColorFilterProperty.mIsSet) {
      drawable.setColorFilter(mColorFilterProperty.mValue);
    }
    if (mDitherProperty.mIsSet) {
      drawable.setDither(mDitherProperty.mValue);
    }
    if (mFilterBitmapProperty.mIsSet) {
      drawable.setFilterBitmap(mFilterBitmapProperty.mValue);
    }
  }

  private static class Property<T> {

    private T mValue;
    private boolean mIsSet;

    public void setValue(T value) {
      mValue = value;
      mIsSet = true;
    }
  }
}
