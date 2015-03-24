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
import android.graphics.Canvas;
import android.graphics.Paint;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/**
 * A shadow of {@link Canvas} that works with the tests in this package.
 */
@Implements(Canvas.class)
public class MyShadowCanvas {

  @RealObject private Canvas mRealCanvas;
  private Bitmap mBitmap;

  public void __constructor__(Bitmap bitmap) {
    mBitmap = bitmap;
  }

  @Implementation
  public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
    for (int x = 0; x < bitmap.getWidth(); x++) {
      for (int y = 0; y < bitmap.getHeight(); y++) {
        mBitmap.setPixel(x, y, bitmap.getPixel(x, y));
      }
    }
  }
}
