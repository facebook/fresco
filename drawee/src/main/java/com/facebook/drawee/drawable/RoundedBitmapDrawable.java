/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;

public class RoundedBitmapDrawable extends RoundedDrawable {

  private final Paint mPaint = new Paint();
  private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  @Nullable private final Bitmap mBitmap;
  private WeakReference<Bitmap> mLastBitmap;

  public RoundedBitmapDrawable(Resources res, @Nullable Bitmap bitmap, @Nullable Paint paint) {
    super(new BitmapDrawable(res, bitmap));
    mBitmap = bitmap;
    if (paint != null) {
      mPaint.set(paint);
    }

    mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    mBorderPaint.setStyle(Paint.Style.STROKE);
  }

  public RoundedBitmapDrawable(Resources res, Bitmap bitmap) {
    this(res, bitmap, null);
  }

  @Override
  public void draw(Canvas canvas) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("RoundedBitmapDrawable#draw");
    }
    if (!shouldRound()) {
      super.draw(canvas);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
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
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  private void updatePaint() {
    if (mLastBitmap == null || mLastBitmap.get() != mBitmap) {
      mLastBitmap = new WeakReference<>(mBitmap);
      mPaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
      mIsShaderTransformDirty = true;
    }
    if (mIsShaderTransformDirty) {
      mPaint.getShader().setLocalMatrix(mTransform);
      mIsShaderTransformDirty = false;
    }
    mPaint.setFilterBitmap(getPaintFilterBitmap());
  }

  /**
   * Creates a new RoundedBitmapDrawable from the given BitmapDrawable.
   *
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
   * If both the radii and border width are zero or bitmap is null, there is nothing to round.
   */
  @VisibleForTesting
  boolean shouldRound() {
    return super.shouldRound() && mBitmap != null;
  }

  @Override
  public void setAlpha(int alpha) {
    super.setAlpha(alpha);
    if (alpha != mPaint.getAlpha()) {
      mPaint.setAlpha(alpha);
      super.setAlpha(alpha);
      invalidateSelf();
    }
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    super.setColorFilter(colorFilter);
    mPaint.setColorFilter(colorFilter);
  }

  Paint getPaint() {
    return mPaint;
  }

}
