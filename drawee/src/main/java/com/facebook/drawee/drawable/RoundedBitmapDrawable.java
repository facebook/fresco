/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import javax.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
* A drawable that can have rounded corners.
*/
public class RoundedBitmapDrawable extends BitmapDrawable
    implements TransformAwareDrawable, Rounded {
  private boolean mIsCircle = false;
  private boolean mRadiiNonZero = false;
  private final float[] mCornerRadii = new float[8];
  @VisibleForTesting final float[] mBorderRadii = new float[8];

  @VisibleForTesting final RectF mRootBounds = new RectF();
  @VisibleForTesting final RectF mPrevRootBounds = new RectF();
  @VisibleForTesting final RectF mBitmapBounds = new RectF();
  @VisibleForTesting final RectF mDrawableBounds = new RectF();

  @VisibleForTesting final Matrix mBoundsTransform = new Matrix();
  @VisibleForTesting final Matrix mPrevBoundsTransform = new Matrix();

  @VisibleForTesting final Matrix mParentTransform = new Matrix();
  @VisibleForTesting final Matrix mPrevParentTransform = new Matrix();
  @VisibleForTesting final Matrix mInverseParentTransform = new Matrix();

  @VisibleForTesting final Matrix mTransform = new Matrix();
  private float mBorderWidth = 0;
  private int mBorderColor = Color.TRANSPARENT;
  private float mPadding = 0;

  private final Path mPath = new Path();
  private final Path mBorderPath = new Path();
  private boolean mIsPathDirty = true;
  private final Paint mPaint = new Paint();
  private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private boolean mIsShaderTransformDirty = true;
  private WeakReference<Bitmap> mLastBitmap;

  private @Nullable TransformCallback mTransformCallback;

  public RoundedBitmapDrawable(Resources res, Bitmap bitmap) {
    this(res, bitmap, null);
  }

  public RoundedBitmapDrawable(Resources res, Bitmap bitmap, @Nullable Paint paint) {
    super(res, bitmap);
    if (paint != null) {
      mPaint.set(paint);
    }

    mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    mBorderPaint.setStyle(Paint.Style.STROKE);
  }

  /**
   * Creates a new RoundedBitmapDrawable from the given BitmapDrawable.
   * @param res resources to use for this drawable
   * @param bitmapDrawable bitmap drawable containing the bitmap to be used for this drawable
   * @return the RoundedBitmapDrawable that is created
   */
  public static RoundedBitmapDrawable fromBitmapDrawable(
      Resources res,
      BitmapDrawable bitmapDrawable) {
    return new RoundedBitmapDrawable(res, bitmapDrawable.getBitmap(), bitmapDrawable.getPaint());
  }

  /**
   * Sets whether to round as circle.
   *
   * @param isCircle whether or not to round as circle
   */
  @Override
  public void setCircle(boolean isCircle) {
    mIsCircle = isCircle;
    mIsPathDirty = true;
    invalidateSelf();
  }

  /** Returns whether or not this drawable rounds as circle. */
  @Override
  public boolean isCircle() {
    return mIsCircle;
  }

  /**
   * Specify radius for the corners of the rectangle. If this is > 0, then the
   * drawable is drawn in a round-rectangle, rather than a rectangle.
   * @param radius the radius for the corners of the rectangle
   */
  @Override
  public void setRadius(float radius) {
    Preconditions.checkState(radius >= 0);
    Arrays.fill(mCornerRadii, radius);
    mRadiiNonZero = (radius != 0);
    mIsPathDirty = true;
    invalidateSelf();
  }

  /**
   * Specify radii for each of the 4 corners. For each corner, the array
   * contains 2 values, [X_radius, Y_radius]. The corners are ordered
   * top-left, top-right, bottom-right, bottom-left
   * @param radii the x and y radii of the corners
   */
  @Override
  public void setRadii(float[] radii) {
    if (radii == null) {
      Arrays.fill(mCornerRadii, 0);
      mRadiiNonZero = false;
    } else {
      Preconditions.checkArgument(radii.length == 8, "radii should have exactly 8 values");
      System.arraycopy(radii, 0, mCornerRadii, 0, 8);
      mRadiiNonZero = false;
      for (int i = 0; i < 8; i++) {
        mRadiiNonZero |= (radii[i] > 0);
      }
    }
    mIsPathDirty = true;
    invalidateSelf();
  }

  /** Gets the radii. */
  @Override
  public float[] getRadii() {
    return mCornerRadii;
  }

  /**
   * Sets the border
   * @param color of the border
   * @param width of the border
   */
  @Override
  public void setBorder(int color, float width) {
    if (mBorderColor != color || mBorderWidth != width) {
      mBorderColor = color;
      mBorderWidth = width;
      mIsPathDirty = true;
      invalidateSelf();
    }
  }

  /** Gets the border color. */
  @Override
  public int getBorderColor() {
    return mBorderColor;
  }

  /** Gets the border width. */
  @Override
  public float getBorderWidth() {
    return mBorderWidth;
  }

  /**
   * Sets the padding for the bitmap.
   * @param padding
   */
  @Override
  public void setPadding(float padding) {
    if (mPadding != padding) {
      mPadding = padding;
      mIsPathDirty = true;
      invalidateSelf();
    }
  }

  /** Gets the padding. */
  @Override
  public float getPadding() {
    return mPadding;
  }

  /**
   * TransformAwareDrawable method
   */
  @Override
  public void setTransformCallback(@Nullable TransformCallback transformCallback) {
    mTransformCallback = transformCallback;
  }

  @Override
  public void setAlpha(int alpha) {
    if (alpha != mPaint.getAlpha()) {
      mPaint.setAlpha(alpha);
      super.setAlpha(alpha);
      invalidateSelf();
    }
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mPaint.setColorFilter(colorFilter);
    super.setColorFilter(colorFilter);
  }

  @Override
  public void draw(Canvas canvas) {
    if (!shouldRound()) {
      super.draw(canvas);
      return;
    }

    updateTransform();
    updatePath();
    updatePaint();
    int saveCount = canvas.save();
    canvas.concat(mInverseParentTransform);
    canvas.drawPath(mPath, mPaint);
    if (mBorderWidth > 0) {
      mBorderPaint.setStrokeWidth(mBorderWidth);
      mBorderPaint.setColor(DrawableUtils.multiplyColorAlpha(mBorderColor, mPaint.getAlpha()));
      canvas.drawPath(mBorderPath, mBorderPaint);
    }
    canvas.restoreToCount(saveCount);
  }

  /**
   * If both the radii and border width are zero, there is nothing to round.
   */
  @VisibleForTesting
  boolean shouldRound() {
    return mIsCircle || mRadiiNonZero || mBorderWidth > 0;
  }

  private void updateTransform() {
    if (mTransformCallback != null) {
      mTransformCallback.getTransform(mParentTransform);
      mTransformCallback.getRootBounds(mRootBounds);
    } else {
      mParentTransform.reset();
      mRootBounds.set(getBounds());
    }

    mBitmapBounds.set(0, 0, getBitmap().getWidth(), getBitmap().getHeight());
    mDrawableBounds.set(getBounds());
    mBoundsTransform.setRectToRect(mBitmapBounds, mDrawableBounds, Matrix.ScaleToFit.FILL);

    if (!mParentTransform.equals(mPrevParentTransform) ||
        !mBoundsTransform.equals(mPrevBoundsTransform)) {
      mIsShaderTransformDirty = true;
      mParentTransform.invert(mInverseParentTransform);
      mTransform.set(mParentTransform);
      mTransform.preConcat(mBoundsTransform);
      mPrevParentTransform.set(mParentTransform);
      mPrevBoundsTransform.set(mBoundsTransform);
    }

    if (!mRootBounds.equals(mPrevRootBounds)) {
      mIsPathDirty = true;
      mPrevRootBounds.set(mRootBounds);
    }
  }

  private void updatePath() {
    if (mIsPathDirty) {
      mBorderPath.reset();
      mRootBounds.inset(mBorderWidth/2, mBorderWidth/2);
      if (mIsCircle) {
        float radius = Math.min(mRootBounds.width(), mRootBounds.height())/2;
        mBorderPath.addCircle(
            mRootBounds.centerX(), mRootBounds.centerY(), radius, Path.Direction.CW);
      } else {
        for (int i = 0; i < mBorderRadii.length; i++) {
          mBorderRadii[i] = mCornerRadii[i] + mPadding - mBorderWidth/2;
        }
        mBorderPath.addRoundRect(mRootBounds, mBorderRadii, Path.Direction.CW);
      }
      mRootBounds.inset(-mBorderWidth/2, -mBorderWidth/2);

      mPath.reset();
      mRootBounds.inset(mPadding, mPadding);
      if (mIsCircle) {
        mPath.addCircle(
            mRootBounds.centerX(),
            mRootBounds.centerY(),
            Math.min(mRootBounds.width(), mRootBounds.height())/2,
            Path.Direction.CW);
      } else {
        mPath.addRoundRect(mRootBounds, mCornerRadii, Path.Direction.CW);
      }
      mRootBounds.inset(-(mPadding), -(mPadding));
      mPath.setFillType(Path.FillType.WINDING);
      mIsPathDirty = false;
    }
  }

  private void updatePaint() {
    Bitmap bitmap = getBitmap();
    if (mLastBitmap == null || mLastBitmap.get() != bitmap) {
      mLastBitmap = new WeakReference<Bitmap>(bitmap);
      mPaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
      mIsShaderTransformDirty = true;
    }
    if (mIsShaderTransformDirty) {
      mPaint.getShader().setLocalMatrix(mTransform);
      mIsShaderTransformDirty = false;
    }
  }
}
