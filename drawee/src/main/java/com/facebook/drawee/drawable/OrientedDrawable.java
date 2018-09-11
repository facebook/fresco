/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * Drawable that automatically rotates the underlying drawable with a pivot in the center of the
 * drawable bounds based on a rotation angle.
 */
public class OrientedDrawable extends ForwardingDrawable {

  @VisibleForTesting final Matrix mRotationMatrix;
  private int mRotationAngle;
  private int mExifOrientation;

  // Temporary objects preallocated in advance to save future allocations.
  private final Matrix mTempMatrix = new Matrix();
  private final RectF mTempRectF = new RectF();

  /**
   * Creates a new OrientedDrawable.
   *
   * @param rotationAngle multiples of 90 or -1 if the angle is unknown
   */
  public OrientedDrawable(Drawable drawable, int rotationAngle) {
    this(drawable, rotationAngle, ExifInterface.ORIENTATION_UNDEFINED);
  }

  /**
   * Creates a new OrientedDrawable.
   *
   * @param rotationAngle multiples of 90 or -1 if the angle is unknown
   * @param exifOrientation EXIF values (1-8), or 0 if unknown
   */
  public OrientedDrawable(Drawable drawable, int rotationAngle, int exifOrientation) {
    super(drawable);
    Preconditions.checkArgument(rotationAngle % 90 == 0);
    Preconditions.checkArgument(exifOrientation >= 0 && exifOrientation <= 8);
    mRotationMatrix = new Matrix();
    mRotationAngle = rotationAngle;
    mExifOrientation = exifOrientation;
  }

  @Override
  public void draw(Canvas canvas) {
    if (mRotationAngle <= 0
        && (mExifOrientation == ExifInterface.ORIENTATION_UNDEFINED
            || mExifOrientation == ExifInterface.ORIENTATION_NORMAL)) {
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
    if (mExifOrientation == ExifInterface.ORIENTATION_TRANSPOSE
        || mExifOrientation == ExifInterface.ORIENTATION_TRANSVERSE
        || mRotationAngle % 180 != 0) {
      return super.getIntrinsicHeight();
    } else {
      return super.getIntrinsicWidth();
    }
  }

  @Override
  public int getIntrinsicHeight() {
    if (mExifOrientation == ExifInterface.ORIENTATION_TRANSPOSE
        || mExifOrientation == ExifInterface.ORIENTATION_TRANSVERSE
        || mRotationAngle % 180 != 0) {
      return super.getIntrinsicWidth();
    } else {
      return super.getIntrinsicHeight();
    }
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    Drawable underlyingDrawable = getCurrent();
    if (mRotationAngle > 0
        || (mExifOrientation != ExifInterface.ORIENTATION_UNDEFINED
            && mExifOrientation != ExifInterface.ORIENTATION_NORMAL)) {
      switch (mExifOrientation) {
        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
          mRotationMatrix.setScale(-1, 1);
          break;
        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
          mRotationMatrix.setScale(1, -1);
          break;
        case ExifInterface.ORIENTATION_TRANSPOSE:
          mRotationMatrix.setRotate(270, bounds.centerX(), bounds.centerY());
          mRotationMatrix.postScale(1, -1);
          break;
        case ExifInterface.ORIENTATION_TRANSVERSE:
          mRotationMatrix.setRotate(270, bounds.centerX(), bounds.centerY());
          mRotationMatrix.postScale(-1, 1);
          break;
        default:
          mRotationMatrix.setRotate(mRotationAngle, bounds.centerX(), bounds.centerY());
          break;
      }

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
