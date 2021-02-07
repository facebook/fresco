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
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public class CircularBorderBitmapDrawable extends BitmapDrawable {

  private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private @Nullable BorderOptions mBorderOptions;
  private int mRadius;
  private int mAlpha = 255;

  public CircularBorderBitmapDrawable(Resources res, @Nullable Bitmap bitmap) {
    super(res, bitmap);
    mBorderPaint.setStyle(Paint.Style.STROKE);
  }

  @Override
  public void draw(Canvas canvas) {
    if (mRadius == 0) return;

    if (mBorderOptions == null || mBorderOptions.padding < 0.0f || mBorderOptions.width < 0.0f) {
      super.draw(canvas);
      return;
    }

    float widthReduction =
        mBorderOptions.scaleDownInsideBorders
            ? mBorderOptions.width + mBorderOptions.padding
            : mBorderOptions.padding;

    if (widthReduction > mRadius) return;

    float centerX = getBounds().exactCenterX();
    float centerY = getBounds().exactCenterY();

    if (widthReduction > 0.0f) {
      float scale = (mRadius - widthReduction) / mRadius;
      canvas.save();
      canvas.scale(scale, scale, centerX, centerY);
      super.draw(canvas);
      canvas.restore();
    } else {
      super.draw(canvas);
    }

    if (mBorderOptions.width > 0.0f) {
      canvas.drawCircle(centerX, centerY, mRadius - mBorderOptions.width / 2, mBorderPaint);
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

  public @Nullable BorderOptions getBorder() {
    return mBorderOptions;
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
