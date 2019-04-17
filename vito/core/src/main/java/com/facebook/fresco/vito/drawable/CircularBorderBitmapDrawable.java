/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.Nullable;
import com.facebook.drawee.drawable.DrawableUtils;
import com.facebook.fresco.vito.options.BorderOptions;

public class CircularBorderBitmapDrawable extends BitmapDrawable {

  private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private @Nullable BorderOptions mBorderOptions;
  private int mRadius;
  private int mAlpha = 255;

  public CircularBorderBitmapDrawable(Resources res, Bitmap bitmap) {
    super(res, bitmap);
    mBorderPaint.setStyle(Paint.Style.STROKE);
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    if (mBorderOptions != null) {
      canvas.drawCircle(
          getBounds().exactCenterX(),
          getBounds().exactCenterY(),
          mRadius - mBorderOptions.width / 2,
          mBorderPaint);
    }
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    mRadius = Math.min(bounds.width(), bounds.height()) / 2;
  }

  public void setBorder(@Nullable BorderOptions borderOptions) {
    if (mBorderOptions == null || !mBorderOptions.equals(borderOptions)) {
      mBorderOptions = borderOptions;
      ensureBorderPaint();
      invalidateSelf();
    }
  }

  @Override
  public void setAlpha(int alpha) {
    super.setAlpha(alpha);
    mAlpha = alpha;
    ensureBorderPaint();
  }

  private void ensureBorderPaint() {
    if (mBorderOptions != null) {
      mBorderPaint.setStrokeWidth(mBorderOptions.width);
      mBorderPaint.setColor(DrawableUtils.multiplyColorAlpha(mBorderOptions.color, mAlpha));
    }
  }
}
