/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.testing;

import android.graphics.Bitmap;

import org.robolectric.Robolectric;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * A shadow of {@link Bitmap} that works with the tests in this package.
 */
@Implements(Bitmap.class)
public class MyShadowBitmap {

  private int width;
  private int height;
  private int[] mPixels;

  @Implementation
  public int getWidth() {
    return width;
  }

  @Implementation
  public int getHeight() {
    return height;
  }

  @Implementation
  public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
    Bitmap bitmap = Robolectric.newInstanceOf(Bitmap.class);
    MyShadowBitmap shadowBitmap = Robolectric.shadowOf_(bitmap);
    shadowBitmap.width = width;
    shadowBitmap.height = height;
    shadowBitmap.mPixels = new int[width * height];
    return bitmap;
  }

  @Implementation
  public static Bitmap createBitmap(int colors[], int width, int height, Bitmap.Config config) {
    Bitmap bitmap = Robolectric.newInstanceOf(Bitmap.class);
    MyShadowBitmap shadowBitmap = Robolectric.shadowOf_(bitmap);
    shadowBitmap.width = width;
    shadowBitmap.height = height;
    shadowBitmap.mPixels = new int[width * height];
    for (int i = 0; i < colors.length; i++) {
      shadowBitmap.mPixels[i] = colors[i];
    }
    return bitmap;
  }

  @Implementation
  public void setPixel(int x, int y, int color) {
    mPixels[y * width + x] = color;
  }

  @Implementation
  public int getPixel(int x, int y) {
    return mPixels[y * width + x];
  }

  @Implementation
  public void eraseColor(int c) {
    for (int i = 0; i < mPixels.length; i++) {
      mPixels[i] = c;
    }
  }
}
