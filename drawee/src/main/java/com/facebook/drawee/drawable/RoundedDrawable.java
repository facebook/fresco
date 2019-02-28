/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.util.Arrays;

public abstract class RoundedDrawable extends Drawable
    implements Rounded, TransformAwareDrawable {

  private final Drawable mDelegate;

  /**
   * Constructs a new forwarding drawable.
   *
   * @param drawable drawable that this forwarding drawable will forward to
   */
  RoundedDrawable(Drawable drawable) {
    mDelegate = drawable;
  }

  protected boolean mIsCircle = false;
  protected boolean mRadiiNonZero = false;
  protected float mBorderWidth = 0;
  protected final Path mPath = new Path();
  protected boolean mIsShaderTransformDirty = true;
  protected int mBorderColor = Color.TRANSPARENT;
  protected final Path mBorderPath = new Path();

  private final float[] mCornerRadii = new float[8];
  @VisibleForTesting final float[] mBorderRadii = new float[8];
  @VisibleForTesting @Nullable float[] mInsideBorderRadii;

  @VisibleForTesting final RectF mRootBounds = new RectF();
  @VisibleForTesting final RectF mPrevRootBounds = new RectF();
  @VisibleForTesting final RectF mBitmapBounds = new RectF();
  @VisibleForTesting final RectF mDrawableBounds = new RectF();
  @VisibleForTesting @Nullable RectF mInsideBorderBounds;

  @VisibleForTesting final Matrix mBoundsTransform = new Matrix();
  @VisibleForTesting final Matrix mPrevBoundsTransform = new Matrix();

  @VisibleForTesting final Matrix mParentTransform = new Matrix();
  @VisibleForTesting final Matrix mPrevParentTransform = new Matrix();
  @VisibleForTesting final Matrix mInverseParentTransform = new Matrix();

  @VisibleForTesting @Nullable Matrix mInsideBorderTransform;
  @VisibleForTesting @Nullable Matrix mPrevInsideBorderTransform;

  @VisibleForTesting final Matrix mTransform = new Matrix();
  private float mPadding = 0;
  private boolean mScaleDownInsideBorders = false;
  private boolean mPaintFilterBitmap = false;

  private boolean mIsPathDirty = true;

  private @Nullable TransformCallback mTransformCallback;

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
   * Sets whether image should be scaled down inside borders.
   *
   * @param scaleDownInsideBorders
   */
  @Override
  public void setScaleDownInsideBorders(boolean scaleDownInsideBorders) {
    if (mScaleDownInsideBorders != scaleDownInsideBorders) {
      mScaleDownInsideBorders = scaleDownInsideBorders;
      mIsPathDirty = true;
      invalidateSelf();
    }
  }

  /** Gets whether image should be scaled down inside borders. */
  @Override
  public boolean getScaleDownInsideBorders() {
    return mScaleDownInsideBorders;
  }

  /**
   * Sets FILTER_BITMAP_FLAG flag to Paint. {@link android.graphics.Paint#FILTER_BITMAP_FLAG}
   *
   * <p>This should generally be on when drawing bitmaps, unless performance-bound (rendering to software
   * canvas) or preferring pixelation artifacts to blurriness when scaling
   * significantly.
   *
   * @param paintFilterBitmap whether to set FILTER_BITMAP_FLAG flag to Paint.
   */
  @Override
  public void setPaintFilterBitmap(boolean paintFilterBitmap) {
    if (mPaintFilterBitmap != paintFilterBitmap) {
      mPaintFilterBitmap = paintFilterBitmap;
      invalidateSelf();
    }
  }

  /** Gets whether to set FILTER_BITMAP_FLAG flag to Paint. */
  @Override
  public boolean getPaintFilterBitmap() {
    return mPaintFilterBitmap;
  }

  /** TransformAwareDrawable method */
  @Override
  public void setTransformCallback(@Nullable TransformCallback transformCallback) {
    mTransformCallback = transformCallback;
  }

  protected void updateTransform() {
    if (mTransformCallback != null) {
      mTransformCallback.getTransform(mParentTransform);
      mTransformCallback.getRootBounds(mRootBounds);
    } else {
      mParentTransform.reset();
      mRootBounds.set(getBounds());
    }

    mBitmapBounds.set(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
    mDrawableBounds.set(mDelegate.getBounds());
    mBoundsTransform.setRectToRect(mBitmapBounds, mDrawableBounds, Matrix.ScaleToFit.FILL);
    if (mScaleDownInsideBorders) {
      if (mInsideBorderBounds == null) {
        mInsideBorderBounds = new RectF(mRootBounds);
      } else {
        mInsideBorderBounds.set(mRootBounds);
      }
      mInsideBorderBounds.inset(mBorderWidth, mBorderWidth);
      if (mInsideBorderTransform == null) {
        mInsideBorderTransform = new Matrix();
      }
      mInsideBorderTransform.setRectToRect(
          mRootBounds, mInsideBorderBounds, Matrix.ScaleToFit.FILL);
    } else if (mInsideBorderTransform != null) {
      mInsideBorderTransform.reset();
    }

    if (!mParentTransform.equals(mPrevParentTransform)
        || !mBoundsTransform.equals(mPrevBoundsTransform)
        || (mInsideBorderTransform != null
        && !mInsideBorderTransform.equals(mPrevInsideBorderTransform))) {
      mIsShaderTransformDirty = true;

      mParentTransform.invert(mInverseParentTransform);
      mTransform.set(mParentTransform);
      if (mScaleDownInsideBorders) {
        mTransform.postConcat(mInsideBorderTransform);
      }
      mTransform.preConcat(mBoundsTransform);

      mPrevParentTransform.set(mParentTransform);
      mPrevBoundsTransform.set(mBoundsTransform);
      if (mScaleDownInsideBorders) {
        if (mPrevInsideBorderTransform == null) {
          mPrevInsideBorderTransform = new Matrix(mInsideBorderTransform);
        } else {
          mPrevInsideBorderTransform.set(mInsideBorderTransform);
        }
      } else if (mPrevInsideBorderTransform != null) {
        mPrevInsideBorderTransform.reset();
      }
    }

    if (!mRootBounds.equals(mPrevRootBounds)) {
      mIsPathDirty = true;
      mPrevRootBounds.set(mRootBounds);
    }
  }

  protected void updatePath() {
    if (mIsPathDirty) {
      mBorderPath.reset();
      mRootBounds.inset(mBorderWidth / 2, mBorderWidth / 2);
      if (mIsCircle) {
        float radius = Math.min(mRootBounds.width(), mRootBounds.height()) / 2;
        mBorderPath.addCircle(
            mRootBounds.centerX(), mRootBounds.centerY(), radius, Path.Direction.CW);
      } else {
        for (int i = 0; i < mBorderRadii.length; i++) {
          mBorderRadii[i] = mCornerRadii[i] + mPadding - mBorderWidth / 2;
        }
        mBorderPath.addRoundRect(mRootBounds, mBorderRadii, Path.Direction.CW);
      }
      mRootBounds.inset(-mBorderWidth / 2, -mBorderWidth / 2);

      mPath.reset();
      float totalPadding = mPadding + (mScaleDownInsideBorders ? mBorderWidth : 0);
      mRootBounds.inset(totalPadding, totalPadding);
      if (mIsCircle) {
        mPath.addCircle(
            mRootBounds.centerX(),
            mRootBounds.centerY(),
            Math.min(mRootBounds.width(), mRootBounds.height()) / 2,
            Path.Direction.CW);
      } else if (mScaleDownInsideBorders) {
        if (mInsideBorderRadii == null) {
          mInsideBorderRadii = new float[8];
        }
        for (int i = 0; i < mBorderRadii.length; i++) {
          mInsideBorderRadii[i] = mCornerRadii[i] - mBorderWidth;
        }
        mPath.addRoundRect(mRootBounds, mInsideBorderRadii, Path.Direction.CW);
      } else {
        mPath.addRoundRect(mRootBounds, mCornerRadii, Path.Direction.CW);
      }
      mRootBounds.inset(-(totalPadding), -(totalPadding));
      mPath.setFillType(Path.FillType.WINDING);
      mIsPathDirty = false;
    }
  }

  /**
   * If both the radii and border width are zero, there is nothing to round.
   */
  @VisibleForTesting
  boolean shouldRound() {
    return (mIsCircle || mRadiiNonZero || mBorderWidth > 0);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    mDelegate.setBounds(bounds);
  }

  @Override
  public int getIntrinsicWidth() {
    return mDelegate.getIntrinsicWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return mDelegate.getIntrinsicHeight();
  }

  @Override
  public int getOpacity() {
    return mDelegate.getOpacity();
  }

  @Override
  public void setColorFilter(
      int color, @NonNull PorterDuff.Mode mode) {
    mDelegate.setColorFilter(color, mode);
  }

  @Override
  public void setColorFilter(
      @Nullable ColorFilter colorFilter) {
    mDelegate.setColorFilter(colorFilter);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Nullable
  @Override
  public ColorFilter getColorFilter() {
    return mDelegate.getColorFilter();
  }

  @Override
  public void clearColorFilter() {
    mDelegate.clearColorFilter();
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  @Override
  public int getAlpha() {
    return mDelegate.getAlpha();
  }

  @Override
  public void setAlpha(int alpha) {
    mDelegate.setAlpha(alpha);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("RoundedDrawable#draw");
    }
    mDelegate.draw(canvas);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }
}
