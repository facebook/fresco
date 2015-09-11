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
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * Drawable that automatically rotates the underlying drawable with a pivot in the center of the
 * drawable bounds based on a rotation angle.
 */
public class OrientedDrawable extends ForwardingDrawable {

  @VisibleForTesting final Matrix mRotationMatrix;
  private int mRotationAngle;

  // Temporary objects preallocated in advance to save future allocations.
  private final Matrix mTempMatrix = new Matrix();
  private final RectF mTempRectF = new RectF();

  /**
   * Creates a new OrientedDrawable. The only rotation angles allowed are multiples of 90 or -1 if
   * the angle is unknown.
   */
  public OrientedDrawable(Drawable drawable, int rotationAngle) {
    super(drawable);
    Preconditions.checkArgument(rotationAngle % 90 == 0);
    mRotationMatrix = new Matrix();
    mRotationAngle = rotationAngle;
  }

  @Override
  public void draw(Canvas canvas) {
    if (mRotationAngle <= 0) {
      super.draw(canvas);
      return;
    }
    int saveCount = canvas.save();
    canvas.concat(mRotationMatrix);
    super.draw(canvas);
    canvas.restoreToCount(saveCount);
  }

  @Override
  public int getIntrinsicWidth() {
    return (mRotationAngle % 180 == 0) ? super.getIntrinsicWidth() : super.getIntrinsicHeight();
  }

  @Override
  public int getIntrinsicHeight() {
    return (mRotationAngle % 180 == 0) ? super.getIntrinsicHeight() : super.getIntrinsicWidth();
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    Drawable underlyingDrawable = getCurrent();
    if (mRotationAngle > 0) {
      mRotationMatrix.setRotate(mRotationAngle, bounds.centerX(), bounds.centerY());
      // Set the rotated bounds on the underlying drawable
      mTempMatrix.reset();
      mRotationMatrix.invert(mTempMatrix);
      mTempRectF.set(bounds);
      mTempMatrix.mapRect(mTempRectF);
      underlyingDrawable.setBounds(
          (int) mTempRectF.left,
          (int) mTempRectF.top,
          (int) mTempRectF.right,
          (int) mTempRectF.bottom);
    } else {
      underlyingDrawable.setBounds(bounds);
    }
  }

  @Override
  public void getTransform(Matrix transform) {
    getParentTransform(transform);
    if (!mRotationMatrix.isIdentity()) {
      transform.preConcat(mRotationMatrix);
    }
  }
}
