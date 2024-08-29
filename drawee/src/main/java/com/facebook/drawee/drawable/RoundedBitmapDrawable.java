/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.VisibleForTesting;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class RoundedBitmapDrawable extends RoundedDrawable {

  private static boolean sDefaultRepeatEdgePixels = false;

  private final Paint mPaint = new Paint();
  private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  @Nullable private final Bitmap mBitmap;
  @Nullable private WeakReference<Bitmap> mLastBitmap;
  private boolean mRepeatEdgePixels;
  private @Nullable RectF mBitmapClipRect = null;

  public static void setDefaultRepeatEdgePixels(boolean defaultRepeatEdgePixels) {
    sDefaultRepeatEdgePixels = defaultRepeatEdgePixels;
  }

  public static boolean getDefaultRepeatEdgePixels() {
    return sDefaultRepeatEdgePixels;
  }

  public RoundedBitmapDrawable(
      Resources res, @Nullable Bitmap bitmap, @Nullable Paint paint, boolean repeatEdgePixels) {
    super(new BitmapDrawable(res, bitmap));
    mBitmap = bitmap;
    if (paint != null) {
      mPaint.set(paint);
    }

    mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    mBorderPaint.setStyle(Paint.Style.STROKE);
    mRepeatEdgePixels = repeatEdgePixels;
  }

  public RoundedBitmapDrawable(Resources res, @Nullable Bitmap bitmap, @Nullable Paint paint) {
    this(res, bitmap, paint, sDefaultRepeatEdgePixels);
  }

  public RoundedBitmapDrawable(Resources res, @Nullable Bitmap bitmap) {
    this(res, bitmap, null);
  }

  @Override
  protected void updateTransform() {
    super.updateTransform();
    if (!mRepeatEdgePixels) {
      if (mBitmapClipRect == null) {
        mBitmapClipRect = new RectF();
      }
      mTransform.mapRect(mBitmapClipRect, mBitmapBounds);
    }
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
    if (!mRepeatEdgePixels && mBitmapClipRect != null) {
      int saveCount2 = canvas.save();
      canvas.clipRect(mBitmapClipRect);
      canvas.drawPath(mPath, mPaint);
      canvas.restoreToCount(saveCount2);
    } else {
      canvas.drawPath(mPath, mPaint);
    }
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
      if (mBitmap != null) {
        mPaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        mIsShaderTransformDirty = true;
      }
    }
    if (mIsShaderTransformDirty) {
      Shader shader = mPaint.getShader();
      if (shader != null) {
        shader.setLocalMatrix(mTransform);
        mIsShaderTransformDirty = false;
      }
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
      Resources res, BitmapDrawable bitmapDrawable) {
    return new RoundedBitmapDrawable(res, bitmapDrawable.getBitmap(), bitmapDrawable.getPaint());
  }

  /** If both the radii and border width are zero or bitmap is null, there is nothing to round. */
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
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    super.setColorFilter(colorFilter);
    mPaint.setColorFilter(colorFilter);
  }

  public Paint getPaint() {
    return mPaint;
  }

  @Override
  public void setRepeatEdgePixels(boolean repeatEdgePixels) {
    mRepeatEdgePixels = repeatEdgePixels;
  }

  public @Nullable Bitmap getBitmap() {
    return mBitmap;
  }

  public boolean getRepeatEdgePixels() {
    return mRepeatEdgePixels;
  }
}
