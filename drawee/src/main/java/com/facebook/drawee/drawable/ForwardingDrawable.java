/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import javax.annotation.Nullable;

/**
 * A forwarding drawable class - the goal is to forward (delegate) drawable functionality to an
 * inner drawable instance. ForwardingDrawable intercepts the public (and protected) methods of
 * {@link Drawable}, maintains local state if needed.
 * <p>
 * Design note: It would have been very helpful to re-use Android library classes
 * like DrawableContainer, LevelListDrawable etc. DrawableContainer is not directly subclassable,
 * and the others don't allow changing the member drawables.
 */
public class ForwardingDrawable extends Drawable
    implements Drawable.Callback, TransformCallback, TransformAwareDrawable, DrawableParent {

  /** The current drawable to be drawn by this drawable when drawing is needed */
  private @Nullable Drawable mCurrentDelegate;

  private final DrawableProperties mDrawableProperties = new DrawableProperties();

  protected TransformCallback mTransformCallback;

  /**
   * Matrix used to store temporary transform. Drawables should be accessed on UI thread only, and
   * this matrix is used only as a temporary variable so it's safe to be static.
   */
  private static final Matrix sTempTransform = new Matrix();

  /**
   * Constructs a new forwarding drawable.
   *
   * @param drawable drawable that this forwarding drawable will forward to
   */
  public ForwardingDrawable(@Nullable Drawable drawable) {
    mCurrentDelegate = drawable;
    DrawableUtils.setCallbacks(mCurrentDelegate, this, this);
  }

  /**
   * Sets a new drawable to be the delegate, and returns the old one (or null).
   *
   * <p>This method will cause the drawable to be invalidated.
   *
   * @param newDelegate
   * @return the previous delegate
   */
  public @Nullable Drawable setCurrent(@Nullable Drawable newDelegate) {
    Drawable previousDelegate = setCurrentWithoutInvalidate(newDelegate);
    invalidateSelf();
    return previousDelegate;
  }

  /**
   * As {@code setCurrent}, but without invalidating a drawable. Subclasses are responsible to call
   * {@code invalidateSelf} on their own.
   *
   * @param newDelegate
   * @return the previous delegate
   */
  protected @Nullable Drawable setCurrentWithoutInvalidate(@Nullable Drawable newDelegate) {
    Drawable previousDelegate = mCurrentDelegate;
    DrawableUtils.setCallbacks(previousDelegate, null, null);
    DrawableUtils.setCallbacks(newDelegate, null, null);
    DrawableUtils.setDrawableProperties(newDelegate, mDrawableProperties);
    DrawableUtils.copyProperties(newDelegate, this);
    DrawableUtils.setCallbacks(newDelegate, this, this);
    mCurrentDelegate = newDelegate;
    return previousDelegate;
  }

  @Override
  public int getOpacity() {
    if (mCurrentDelegate == null) {
      return PixelFormat.UNKNOWN;
    }

    return mCurrentDelegate.getOpacity();
  }

  @Override
  public void setAlpha(int alpha) {
    mDrawableProperties.setAlpha(alpha);
    if (mCurrentDelegate != null) {
      mCurrentDelegate.setAlpha(alpha);
    }
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mDrawableProperties.setColorFilter(colorFilter);
    if (mCurrentDelegate != null) {
      mCurrentDelegate.setColorFilter(colorFilter);
    }
  }

  @Override
  public void setDither(boolean dither) {
    mDrawableProperties.setDither(dither);
    if (mCurrentDelegate != null) {
      mCurrentDelegate.setDither(dither);
    }
  }

  @Override
  public void setFilterBitmap(boolean filterBitmap) {
    mDrawableProperties.setFilterBitmap(filterBitmap);
    if (mCurrentDelegate != null) {
      mCurrentDelegate.setFilterBitmap(filterBitmap);
    }
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    final boolean superResult = super.setVisible(visible, restart);
    if (mCurrentDelegate == null) {
      return superResult;
    }

    return mCurrentDelegate.setVisible(visible, restart);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    if (mCurrentDelegate != null) {
      mCurrentDelegate.setBounds(bounds);
    }
  }

  @Override
  public @Nullable Drawable.ConstantState getConstantState() {
    if (mCurrentDelegate == null) {
      return super.getConstantState();
    }

    return mCurrentDelegate.getConstantState();
  }

  @Override
  public boolean isStateful() {
    if (mCurrentDelegate == null) {
      return false;
    }

    return mCurrentDelegate.isStateful();
  }

  @Override
  protected boolean onStateChange(int[] state) {
    if (mCurrentDelegate == null) {
      return super.onStateChange(state);
    }

    return mCurrentDelegate.setState(state);
  }

  @Override
  protected boolean onLevelChange(int level) {
    if (mCurrentDelegate == null) {
      return super.onLevelChange(level);
    }

    return mCurrentDelegate.setLevel(level);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mCurrentDelegate != null) {
      mCurrentDelegate.draw(canvas);
    }
  }

  @Override
  public int getIntrinsicWidth() {
    if (mCurrentDelegate == null) {
      return super.getIntrinsicWidth();
    }

    return mCurrentDelegate.getIntrinsicWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    if (mCurrentDelegate == null) {
      return super.getIntrinsicHeight();
    }

    return mCurrentDelegate.getIntrinsicHeight();
  }

  @Override
  public boolean getPadding(Rect padding) {
    if (mCurrentDelegate == null) {
      return super.getPadding(padding);
    }

    return mCurrentDelegate.getPadding(padding);
  }

  @Override
  public Drawable mutate() {
    if (mCurrentDelegate != null) {
      mCurrentDelegate.mutate();
    }

    return this;
  }

  @Override
  public @Nullable Drawable getCurrent() {
    return mCurrentDelegate;
  }

  // DrawableParent methods

  @Override
  public Drawable setDrawable(@Nullable Drawable newDrawable) {
    return setCurrent(newDrawable);
  }

  @Override
  public @Nullable Drawable getDrawable() {
    return getCurrent();
  }

  // Drawable.Callback methods

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

  //  TransformAwareDrawable methods

  @Override
  public void setTransformCallback(TransformCallback transformCallback) {
    mTransformCallback = transformCallback;
  }

  // TransformationCallback methods

  protected void getParentTransform(Matrix transform) {
    if (mTransformCallback != null) {
      mTransformCallback.getTransform(transform);
    } else {
      transform.reset();
    }
  }

  @Override
  public void getTransform(Matrix transform) {
    getParentTransform(transform);
  }

  @Override
  public void getRootBounds(RectF bounds) {
    if (mTransformCallback != null) {
      mTransformCallback.getRootBounds(bounds);
    } else {
      bounds.set(getBounds());
    }
  }

  /**
   * Gets the transformed bounds of this drawable.
   * Note: bounds are not cropped (otherwise they would likely be the same as drawable's bounds).
   * @param outBounds rect to fill with bounds
   */
  public void getTransformedBounds(RectF outBounds) {
    getParentTransform(sTempTransform);
    // IMPORTANT: {@code getBounds} should be called after {@code getParentTransform},
    // because the parent may have to change our bounds.
    outBounds.set(getBounds());
    sTempTransform.mapRect(outBounds);
  }

  @Override
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void setHotspot(float x, float y) {
    if (mCurrentDelegate != null) {
      mCurrentDelegate.setHotspot(x, y);
    }
  }
}
