/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * Drawable that automatically rotates the underlying drawable with a pivot in the center of the
 * drawable bounds based on a rotation angle.
 */
public class OrientedBitmapDrawable extends BitmapDrawable {

  private static final int UNKNOWN_ROTATION_ANGLE = -1;

  @VisibleForTesting final Matrix mRotationMatrix;
  private int mRotationAngle;

  /**
   * Creates a new OrientedBitmapDrawable. The only rotation angles allowed are multiples of 90 or
   * -1 if the angle is unknown.
   */
  public OrientedBitmapDrawable(Resources res, Bitmap bitmap, int rotationAngle) {
    super(res, bitmap);
    Preconditions.checkArgument(rotationAngle == UNKNOWN_ROTATION_ANGLE || rotationAngle % 90 == 0);
    mRotationMatrix = new Matrix();
    mRotationAngle = rotationAngle;
  }

  @Override
  public void draw(Canvas canvas) {
    Rect bounds = getBounds();
    if (mRotationAngle <= 0) {
      super.draw(canvas);
      return;
    }
    mRotationMatrix.setRotate(mRotationAngle, bounds.centerX(), bounds.centerY());
    int saveCount = canvas.save();
    canvas.concat(mRotationMatrix);
    super.draw(canvas);
    canvas.restoreToCount(saveCount);
  }

}
