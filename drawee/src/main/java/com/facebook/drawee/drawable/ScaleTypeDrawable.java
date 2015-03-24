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
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * Drawable that can scale underlying drawable based on specified {@link ScalingUtils.ScaleType}
 * options.
 * <p/> Based on {@link android.widget.ImageView.ScaleType}.
 */
public class ScaleTypeDrawable extends ForwardingDrawable {

  // Specified scale type.
  @VisibleForTesting ScalingUtils.ScaleType mScaleType;

  // Specified focus point to use with FOCUS_CROP.
  @VisibleForTesting PointF mFocusPoint = null;

  // Last known dimensions of the underlying drawable. Used to avoid computing bounds every time
  // if underlying size hasn't changed.
  @VisibleForTesting int mUnderlyingWidth = 0;
  @VisibleForTesting int mUnderlyingHeight = 0;

  // Matrix that is actually being used for drawing.
  @VisibleForTesting Matrix mDrawMatrix;

  // Temporary objects preallocated in advance to save future allocations.
  private Matrix mTempMatrix = new Matrix();

  /**
   * Creates a new ScaleType drawable with given underlying drawable and scale type.
   * @param drawable underlying drawable to apply scale type on
   * @param scaleType scale type to be applied
   */
  public ScaleTypeDrawable(Drawable drawable, ScalingUtils.ScaleType scaleType) {
    super(Preconditions.checkNotNull(drawable));
    mScaleType = scaleType;
  }

  /**
   * Gets the current scale type.
   * @return scale type
   */
  public ScalingUtils.ScaleType getScaleType() {
    return mScaleType;
  }

  /**
   * Sets the scale type.
   * @param scaleType scale type to set
   */
  public void setScaleType(ScalingUtils.ScaleType scaleType) {
    mScaleType = scaleType;
    configureBounds();
    invalidateSelf();
  }

  /**
   * Gets the focus point.
   * @return focus point of the image
   */
  public PointF getFocusPoint() {
    return mFocusPoint;
  }

  /**
   * Sets the focus point.
   * If ScaleType.FOCUS_CROP is used, focus point will attempted to be centered within a view.
   * Each coordinate is a real number in [0,1] range, in the coordinate system where top-left
   * corner of the image corresponds to (0, 0) and the bottom-right corner corresponds to (1, 1).
   * @param focusPoint focus point of the image
   */
  public void setFocusPoint(PointF focusPoint) {
    if (mFocusPoint == null) {
      mFocusPoint = new PointF();
    }
    mFocusPoint.set(focusPoint);
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
   * Adopted from android.widget.ImageView
   */
  @VisibleForTesting void configureBounds() {
    Drawable underlyingDrawable = getCurrent();
    Rect bounds = getBounds();
    int viewWidth = bounds.width();
    int viewHeight = bounds.height();
    int underlyingWidth = mUnderlyingWidth = underlyingDrawable.getIntrinsicWidth();
    int underlyingHeight = mUnderlyingHeight = underlyingDrawable.getIntrinsicHeight();

    // If the drawable has no intrinsic size, we just fill our entire view.
    if (underlyingWidth <= 0 || underlyingHeight <= 0) {
      underlyingDrawable.setBounds(bounds);
      mDrawMatrix = null;
      return;
    }

    // If the drawable fits exactly, no transform needed.
    if (underlyingWidth == viewWidth && underlyingHeight == viewHeight) {
      underlyingDrawable.setBounds(bounds);
      mDrawMatrix = null;
      return;
    }

    // If we're told to scale to fit, we just fill our entire view.
    // (ScalingUtils.getTransform would do, but this is faster)
    if (mScaleType == ScalingUtils.ScaleType.FIT_XY) {
      underlyingDrawable.setBounds(bounds);
      mDrawMatrix = null;
      return;
    }

    // We need to do the scaling ourselves, so have the underlying drawable use its preferred size.
    underlyingDrawable.setBounds(0, 0, underlyingWidth, underlyingHeight);
    ScalingUtils.getTransform(
        mTempMatrix,
        bounds,
        underlyingWidth,
        underlyingHeight,
        (mFocusPoint != null) ? mFocusPoint.x : 0.5f,
        (mFocusPoint != null) ? mFocusPoint.y : 0.5f,
        mScaleType);
    mDrawMatrix = mTempMatrix;
  }

  /**
   * TransformationCallback method
   * @param transform
   */
  @Override
  public void getTransform(Matrix transform) {
    getParentTransform(transform);
    // IMPORTANT: {@code configureBounds} should be called after {@code getParentTransform},
    // because the parent may have to change our bounds.
    configureBoundsIfUnderlyingChanged();
    if (mDrawMatrix != null) {
      transform.preConcat(mDrawMatrix);
    }
  }
}
