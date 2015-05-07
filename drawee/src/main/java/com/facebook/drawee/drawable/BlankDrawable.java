/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/**
 * A drawable that never draws anything.
 */
public class BlankDrawable extends Drawable {

  public static final BlankDrawable INSTANCE = new BlankDrawable();

  @Override
  public void draw(Canvas canvas) {
    // Do nothing.
  }

  @Override
  public void setAlpha(int alpha) {
    // Do nothing.
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    // Do nothing.
  }

  @Override
  public int getOpacity() {
    return 255;
  }

  @Override
  public Callback getCallback() {
    // return null because we don't want to invalidate anyone.
    return null;
  }
}
