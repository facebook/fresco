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

  private static final int UNSET = -1;

  private int mAlpha = UNSET;
  private boolean mIsSetColorFilter = false;
  private ColorFilter mColorFilter = null;
  private int mDither = UNSET;
  private int mFilterBitmap = UNSET;

  public void setAlpha(int alpha) {
    mAlpha = alpha;
  }

  public void setColorFilter(ColorFilter colorFilter) {
    mColorFilter = colorFilter;
    mIsSetColorFilter = true;
  }

  public void setDither(boolean dither) {
    mDither = dither ? 1 : 0;
  }

  public void setFilterBitmap(boolean filterBitmap) {
    mFilterBitmap = filterBitmap ? 1 : 0;
  }

  public void applyTo(Drawable drawable) {
    if (drawable == null) {
      return;
    }
    if (mAlpha != UNSET) {
      drawable.setAlpha(mAlpha);
    }
    if (mIsSetColorFilter) {
      drawable.setColorFilter(mColorFilter);
    }
    if (mDither != UNSET) {
      drawable.setDither(mDither != 0);
    }
    if (mFilterBitmap != UNSET) {
      drawable.setFilterBitmap(mFilterBitmap != 0);
    }
  }
}
