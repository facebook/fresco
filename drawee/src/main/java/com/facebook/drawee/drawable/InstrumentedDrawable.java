/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Used to log image params at draw-time. */
public class InstrumentedDrawable extends ForwardingDrawable {

  public interface Listener {
    void track(
        int viewWidth,
        int viewHeight,
        int imageWidth,
        int imageHeight,
        int scaledWidth,
        int scaledHeight);
  }

  private final Listener mListener;
  private boolean mIsChecked = false;

  public InstrumentedDrawable(Drawable drawable, Listener listener) {
    super(drawable);
    mListener = listener;
  }

  @Override
  public void draw(Canvas canvas) {
    if (!mIsChecked) {
      mIsChecked = true;
      RectF bounds = new RectF();
      getRootBounds(bounds);
      int viewWidth = (int) bounds.width();
      int viewHeight = (int) bounds.height();
      getTransformedBounds(bounds);
      int scaledWidth = (int) bounds.width();
      int scaledHeight = (int) bounds.height();
      int imageWidth = getIntrinsicWidth();
      int imageHeight = getIntrinsicHeight();
      if (mListener != null) {
        mListener.track(viewWidth, viewHeight, imageWidth, imageHeight, scaledWidth, scaledHeight);
      }
    }
    super.draw(canvas);
  }
}
