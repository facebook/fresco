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
import com.facebook.common.internal.Preconditions;
import javax.annotation.Nullable;

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
  // drawable parents for the layers (lazily created)
  private final DrawableParent[] mDrawableParents;

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
    mDrawableParents = new DrawableParent[mLayers.length];
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
  @Nullable
  public Drawable getDrawable(int index) {
    Preconditions.checkArgument(index >= 0);
    Preconditions.checkArgument(index < mLayers.length);
    return mLayers[index];
  }

  /** Sets a new drawable at the specified index, and return the previous drawable, if any. */
  @Nullable
  public Drawable setDrawable(int index, @Nullable Drawable drawable) {
    Preconditions.checkArgument(index >= 0);
    Preconditions.checkArgument(index < mLayers.length);
    final Drawable oldDrawable = mLayers[index];
    if (drawable != oldDrawable) {
      if (drawable != null && mIsMutated) {
        drawable.mutate();
      }

      DrawableUtils.setCallbacks(mLayers[index], null, null);
      DrawableUtils.setCallbacks(drawable, null, null);
      DrawableUtils.setDrawableProperties(drawable, mDrawableProperties);
      DrawableUtils.copyProperties(drawable, this);
      DrawableUtils.setCallbacks(drawable, this, this);
      mIsStatefulCalculated = false;
      mLayers[index] = drawable;
      invalidateSelf();
    }
    return oldDrawable;
  }


  @Override
  public int getIntrinsicWidth() {
    int width = -1;
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        width = Math.max(width, drawable.getIntrinsicWidth());
      }
    }
    return width > 0 ? width : -1;
  }

  @Override
  public int getIntrinsicHeight() {
    int height = -1;
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        height = Math.max(height, drawable.getIntrinsicHeight());
      }
    }
    return height > 0 ? height : -1;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.setBounds(bounds);
      }
    }
  }

  @Override
  public boolean isStateful() {
    if (!mIsStatefulCalculated) {
      mIsStateful = false;
      for (int i = 0; i < mLayers.length; i++) {
        Drawable drawable = mLayers[i];
        mIsStateful |= drawable != null && drawable.isStateful();
      }
      mIsStatefulCalculated = true;
    }
    return mIsStateful;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    boolean stateChanged = false;
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null && drawable.setState(state)) {
        stateChanged = true;
      }
    }
    return stateChanged;
  }

  @Override
  protected boolean onLevelChange(int level) {
    boolean levelChanged = false;
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null && drawable.setLevel(level)) {
        levelChanged = true;
      }
    }
    return levelChanged;
  }

  @Override
  public void draw(Canvas canvas) {
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.draw(canvas);
      }
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
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.getPadding(rect);
        padding.left = Math.max(padding.left, rect.left);
        padding.top = Math.max(padding.top, rect.top);
        padding.right = Math.max(padding.right, rect.right);
        padding.bottom = Math.max(padding.bottom, rect.bottom);
      }
    }
    return true;
  }

  @Override
  public Drawable mutate() {
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.mutate();
      }
    }
    mIsMutated = true;
    return this;
  }

  @Override
  public int getOpacity() {
    if (mLayers.length == 0) {
      return PixelFormat.TRANSPARENT;
    }
    int opacity = PixelFormat.OPAQUE;
    for (int i = 1; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        opacity = Drawable.resolveOpacity(opacity, drawable.getOpacity());
      }
    }
    return opacity;
  }

  @Override
  public void setAlpha(int alpha) {
    mDrawableProperties.setAlpha(alpha);
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.setAlpha(alpha);
      }
    }
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mDrawableProperties.setColorFilter(colorFilter);
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.setColorFilter(colorFilter);
      }
    }
  }

  @Override
  public void setDither(boolean dither) {
    mDrawableProperties.setDither(dither);
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.setDither(dither);
      }
    }
  }

  @Override
  public void setFilterBitmap(boolean filterBitmap) {
    mDrawableProperties.setFilterBitmap(filterBitmap);
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.setFilterBitmap(filterBitmap);
      }
    }
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    boolean changed = super.setVisible(visible, restart);
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.setVisible(visible, restart);
      }
    }
    return changed;
  }

  /**
   * Gets the {@code DrawableParent} for index.
   */
  public DrawableParent getDrawableParentForIndex(int index) {
    Preconditions.checkArgument(index >= 0);
    Preconditions.checkArgument(index < mDrawableParents.length);
    if (mDrawableParents[index] == null) {
      mDrawableParents[index] = createDrawableParentForIndex(index);
    }
    return mDrawableParents[index];
  }

  private DrawableParent createDrawableParentForIndex(final int index) {
    return new DrawableParent() {
      @Override
      public Drawable setDrawable(Drawable newDrawable) {
        return ArrayDrawable.this.setDrawable(index, newDrawable);
      }

      @Override
      public Drawable getDrawable() {
        return ArrayDrawable.this.getDrawable(index);
      }
    };
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

  @Override
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void setHotspot(float x, float y) {
    for (int i = 0; i < mLayers.length; i++) {
      Drawable drawable = mLayers[i];
      if (drawable != null) {
        drawable.setHotspot(x, y);
      }
    }
  }
}
