/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.generic;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.VisibilityAwareDrawable;
import com.facebook.drawee.drawable.VisibilityCallback;
import javax.annotation.Nullable;

/**
 * The root drawable of a DraweeHierarchy.
 *
 * Root drawable has several functions:
 * <ul>
 * <li> A hierarchy always has the same instance of a root drawable. That means that internal
 * structural changes within the hierarchy don't require setting a new drawable to the view.
 * <li> Root drawable prevents intrinsic dimensions to escape the hierarchy. This in turn prevents
 * view to do any erroneous scaling based on those intrinsic dimensions, as the hierarchy is in
 * charge of all the required scaling.
 * <li> Root drawable is visibility aware. Visibility callback is used to attach the controller
 * (if not already attached) when the hierarchy needs to be drawn. This prevents photo-not-loading
 * issues in case attach event has not been called (for whatever reason). It also helps with
 * memory management as the controller will get detached if the drawable is not visible.
 * <li> Root drawable supports controller overlay, a special overlay set by the controller. Typical
 * usages are debugging, diagnostics and other cases where controller-specific overlay is required.
 * </ul>
 */
public class RootDrawable extends ForwardingDrawable implements VisibilityAwareDrawable {

  @VisibleForTesting
  @Nullable
  Drawable mControllerOverlay = null;

  @Nullable
  private VisibilityCallback mVisibilityCallback;

  public RootDrawable(Drawable drawable) {
    super(drawable);
  }

  @Override
  public int getIntrinsicWidth() {
    return -1;
  }

  @Override
  public int getIntrinsicHeight() {
    return -1;
  }

  @Override
  public void setVisibilityCallback(@Nullable VisibilityCallback visibilityCallback) {
    mVisibilityCallback = visibilityCallback;
  }

  @Override
  public boolean setVisible(boolean visible, boolean restart) {
    if (mVisibilityCallback != null) {
      mVisibilityCallback.onVisibilityChange(visible);
    }
    return super.setVisible(visible, restart);
  }

  @SuppressLint("WrongCall")
  @Override
  public void draw(Canvas canvas) {
    if (!isVisible()) {
      return;
    }
    if (mVisibilityCallback != null) {
      mVisibilityCallback.onDraw();
    }
    super.draw(canvas);
    if (mControllerOverlay != null) {
      mControllerOverlay.setBounds(getBounds());
      mControllerOverlay.draw(canvas);
    }
  }

  public void setControllerOverlay(@Nullable Drawable controllerOverlay) {
    mControllerOverlay = controllerOverlay;
    invalidateSelf();
  }
}
