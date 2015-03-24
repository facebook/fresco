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
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Preconditions;

/**
 * A Drawable that contains an array of other Drawables (layers). These are drawn in array order,
 * so the element with the largest index will be drawn on top.
 *
 * <p>Similar to android's LayerDrawable but it doesn't support adding/removing layers dynamically.
 */
public class ArrayDrawable extends Drawable
    implements Drawable.Callback, TransformCallback, TransformAwareDrawable {

  private TransformCallback mTransformCallback;

  private final DrawableProperties mDrawableProperties = new DrawableProperties();

  // layers
  private final Drawable[] mLayers;

  // temp rect to avoid allocations
  private final Rect mTmpRect = new Rect();

  // Whether the drawable is stateful or not
  private boolean mIsStateful = false;
  private boolean mIsStatefulCalculated = false;

  private boolean mIsMutated = false;

  /**
   * Constructs a new layer drawable.
   * @param layers the layers that this drawable displays
   */
  public ArrayDrawable(Drawable[] layers) {
    Preconditions.checkNotNull(layers);
    mLayers = layers;
    for (int i = 0; i < mLayers.length; i++) {
      DrawableUtils.setCallbacks(mLayers[i], this, this);
    }
  }

  /**
   * Gets the number of layers.
   * @return number of layers
   */
  public int getNumberOfLayers() {
    return mLayers.length;
  }

  /**
   * Gets the drawable at the specified index.
   * @param index index of drawable to get
   * @return drawable at the specified index
   */
  public Drawable getDrawable(int index) {
    return mLayers[index];
  }

  /** Sets a new drawable at the specified index. */
  public void setDrawable(int index, Drawable drawable) {
    Preconditions.checkArgument(index >= 0);
    Preconditions.checkArgument(index < mLayers.length);
    if (drawable != mLayers[index]) {
      if (mIsMutated) {
        drawable = drawable.mutate();
      }
      DrawableUtils.setCallbacks(mLayers[index], null, null);
      DrawableUtils.setCallbacks(drawable, null, null);
      DrawableUtils.setDrawableProperties(drawable, mDrawableProperties);
      DrawableUtils.copyProperties(drawable, mLayers[index]);
      DrawableUtils.setCallbacks(drawable, this, this);
      mIsStatefulCalculated = false;
      mLayers[index] = drawable;
      invalidateSelf();
    }
  }


  @Override
  public int getIntrinsicWidth() {
    int width = 0;
    for (int i = 0; i < mLayers.length; i++) {
      width = Math.max(width, mLayers[i].getIntrinsicWidth());
    }
    return width;
  }

  @Override
  public int getIntrinsicHeight() {
    int height = 0;
    for (int i = 0; i < mLayers.length; i++) {
      height = Math.max(height, mLayers[i].getIntrinsicHeight());
    }
    return height;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].setBounds(bounds);
    }
  }

  @Override
  public boolean isStateful() {
    if (!mIsStatefulCalculated) {
      mIsStateful = false;
      for (int i = 0; i < mLayers.length; i++) {
        mIsStateful |= mLayers[i].isStateful();
      }
      mIsStatefulCalculated = true;
    }
    return mIsStateful;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    boolean stateChanged = false;
    for (int i = 0; i < mLayers.length; i++) {
      if (mLayers[i].setState(state)) {
        stateChanged = true;
      }
    }
    return stateChanged;
  }

  @Override
  protected boolean onLevelChange(int level) {
    boolean levelChanged = false;
    for (int i = 0; i < mLayers.length; i++) {
      if (mLayers[i].setLevel(level)) {
        levelChanged = true;
      }
    }
    return levelChanged;
  }

  @Override
  public void draw(Canvas canvas) {
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].draw(canvas);
    }
  }

  @Override
  public boolean getPadding(Rect padding) {
    padding.left = 0;
    padding.top = 0;
    padding.right = 0;
    padding.bottom = 0;
    final Rect rect = mTmpRect;
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].getPadding(rect);
      padding.left = Math.max(padding.left, rect.left);
      padding.top = Math.max(padding.top, rect.top);
      padding.right = Math.max(padding.right, rect.right);
      padding.bottom = Math.max(padding.bottom, rect.bottom);
    }
    return true;
  }

  @Override
  public Drawable mutate() {
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].mutate();
    }
    mIsMutated = true;
    return this;
  }

  @Override
  public int getOpacity() {
    if (mLayers.length == 0) {
      return PixelFormat.TRANSPARENT;
    }
    int opacity = mLayers[0].getOpacity();
    for (int i = 1; i < mLayers.length; i++) {
      opacity = Drawable.resolveOpacity(opacity, mLayers[i].getOpacity());
    }
    return opacity;
  }

  @Override
  public void setAlpha(int alpha) {
    mDrawableProperties.setAlpha(alpha);
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].setAlpha(alpha);
    }
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mDrawableProperties.setColorFilter(colorFilter);
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].setColorFilter(colorFilter);
    }
  }

  @Override
  public void setDither(boolean dither) {
    mDrawableProperties.setDither(dither);
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].setDither(dither);
    }
  }

  @Override
  public void setFilterBitmap(boolean filterBitmap) {
    mDrawableProperties.setFilterBitmap(filterBitmap);
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].setFilterBitmap(filterBitmap);
    }
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    boolean changed = super.setVisible(visible, restart);
    for (int i = 0; i < mLayers.length; i++) {
      mLayers[i].setVisible(visible, restart);
    }
    return changed;
  }

  /**
   * Drawable.Callback methods
   */

  @Override
  public void invalidateDrawable(Drawable who) {
    invalidateSelf();
  }

  @Override
  public void scheduleDrawable(Drawable who, Runnable what, long when) {
    scheduleSelf(what, when);
  }

  @Override
  public void unscheduleDrawable(Drawable who, Runnable what) {
    unscheduleSelf(what);
  }

  /**
   * TransformationCallbackSetter method
   */
  @Override
  public void setTransformCallback(TransformCallback transformCallback) {
    mTransformCallback = transformCallback;
  }

  /**
   * TransformationCallback methods
   */

  @Override
  public void getTransform(Matrix transform) {
    if (mTransformCallback != null) {
      mTransformCallback.getTransform(transform);
    } else {
      transform.reset();
    }
  }

  @Override
  public void getRootBounds(RectF bounds) {
    if (mTransformCallback != null) {
      mTransformCallback.getRootBounds(bounds);
    } else {
      bounds.set(getBounds());
    }
  }
}
