/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A Drawable that draws nothing on Canvas
 */
public class NopDrawable extends Drawable {

  public static final NopDrawable INSTANCE = new NopDrawable();

  @Override
  public void draw(@NonNull Canvas canvas) {
    // nop
  }

  @Override
  public void setAlpha(int alpha) {
    // nop
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    // nop
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
