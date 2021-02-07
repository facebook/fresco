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
import com.facebook.infer.annotation.Nullsafe;

/** Used to log image params at draw-time. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class InstrumentedDrawable extends ForwardingDrawable {

  private final String mScaleType;

  public interface Listener {
    void track(
        int viewWidth,
        int viewHeight,
        int imageWidth,
        int imageHeight,
        int scaledWidth,
        int scaledHeight,
        String scaleType);
  }

  private final Listener mListener;
  private boolean mIsChecked = false;

  public InstrumentedDrawable(Drawable drawable, Listener listener) {
    super(drawable);
    mListener = listener;
    mScaleType = getScaleType(drawable);
  }

  private String getScaleType(Drawable drawable) {
    if (drawable instanceof ScaleTypeDrawable) {
      ScalingUtils.ScaleType type = ((ScaleTypeDrawable) drawable).getScaleType();
      return type.toString();
    }
    return "none";
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
        mListener.track(
            viewWidth, viewHeight, imageWidth, imageHeight, scaledWidth, scaledHeight, mScaleType);
      }
    }
    super.draw(canvas);
  }
}
