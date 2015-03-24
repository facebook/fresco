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
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;

/**
 * Drawable that can adjust underlying drawable based on specified {@link Matrix}.
 */
public class MatrixDrawable extends ForwardingDrawable {

  // Specified matrix.
  private Matrix mMatrix;

  // Matrix that is actually being used for drawing. In case underlying drawable doesn't have
  // intrinsic dimensions, this will be null (i.e. no matrix will be applied).
  private Matrix mDrawMatrix;

  // Last known dimensions of the underlying drawable. Used to avoid computing bounds every time
  // if underlying size hasn't changed.
  private int mUnderlyingWidth = 0;
  private int mUnderlyingHeight = 0;

  /**
   * Creates a new MatrixDrawable with given underlying drawable and matrix.
   * @param drawable underlying drawable to apply the matrix to
   * @param matrix matrix to be applied to the drawable
   */
  public MatrixDrawable(Drawable drawable, Matrix matrix) {
    super(Preconditions.checkNotNull(drawable));
    mMatrix = matrix;
  }

  /**
   * Gets the current matrix.
   * @return matrix
   */
  public Matrix getMatrix() {
    return mMatrix;
  }

  /**
   * Sets the matrix.
   * @param matrix matrix to set
   */
  public void setMatrix(Matrix matrix) {
    mMatrix = matrix;
    configureBounds();
    invalidateSelf();
  }

  @Override
  public void draw(Canvas canvas) {
    configureBoundsIfUnderlyingChanged();
    if (mDrawMatrix != null) {
      int saveCount = canvas.save();
      canvas.clipRect(getBounds());
      canvas.concat(mDrawMatrix);
      super.draw(canvas);
      canvas.restoreToCount(saveCount);
    } else {
      // mDrawMatrix == null means our bounds match and we can take fast path
      super.draw(canvas);
    }
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    configureBounds();
  }

  private void configureBoundsIfUnderlyingChanged() {
    if (mUnderlyingWidth != getCurrent().getIntrinsicWidth() ||
        mUnderlyingHeight != getCurrent().getIntrinsicHeight()) {
      configureBounds();
    }
  }

  /**
   * Determines bounds for the underlying drawable and a matrix that should be applied on it.
   */
  private void configureBounds() {
    Drawable underlyingDrawable = getCurrent();
    Rect bounds = getBounds();
    int underlyingWidth = mUnderlyingWidth = underlyingDrawable.getIntrinsicWidth();
    int underlyingHeight = mUnderlyingHeight = underlyingDrawable.getIntrinsicHeight();

    // In case underlying drawable doesn't have intrinsic dimensions, we cannot set its bounds to
    // -1 so we use our bounds and discard specified matrix. In normal case we use drawable's
    // intrinsic dimensions for its bounds and apply specified matrix to it.
    if (underlyingWidth <= 0 || underlyingHeight <= 0) {
      underlyingDrawable.setBounds(bounds);
      mDrawMatrix = null;
    } else {
      underlyingDrawable.setBounds(0, 0, underlyingWidth, underlyingHeight);
      mDrawMatrix = mMatrix;
    }
  }

  /**
   * TransformationCallback method
   * @param transform
   */
  @Override
  public void getTransform(Matrix transform) {
    super.getTransform(transform);
    if (mDrawMatrix != null) {
      transform.preConcat(mDrawMatrix);
    }
  }
}
