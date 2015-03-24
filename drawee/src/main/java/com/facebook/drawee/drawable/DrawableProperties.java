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

public class DrawableProperties {

  private int mAlpha = 255;
  private ColorFilter mColorFilter = null;
  private boolean mDither = true;
  private boolean mFilterBitmap = true;

  public int getAlpha() {
    return mAlpha;
  }

  public void setAlpha(int alpha) {
    mAlpha = alpha;
  }

  public ColorFilter getColorFilter() {
    return mColorFilter;
  }

  public void setColorFilter(ColorFilter colorFilter) {
    mColorFilter = colorFilter;
  }

  public boolean isDither() {
    return mDither;
  }

  public void setDither(boolean dither) {
    mDither = dither;
  }

  public boolean isFilterBitmap() {
    return mFilterBitmap;
  }

  public void setFilterBitmap(boolean filterBitmap) {
    mFilterBitmap = filterBitmap;
  }
}
