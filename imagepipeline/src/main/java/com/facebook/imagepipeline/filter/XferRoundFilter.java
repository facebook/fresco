/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import androidx.annotation.RequiresApi;
import com.facebook.common.internal.Preconditions;

/**
 * Not-in-place rounding image algorithm using {@link Canvas} that requires an Android version >=
 * 3.1. If image quality is preferred above performance, this algorithm performs anti-aliasing and
 * will generate better looking images, otherwise clients that look for better performances should
 * use NativeRoundingFilter.
 */
public final class XferRoundFilter {

  private XferRoundFilter() {}

  @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR1)
  public static void xferRoundBitmap(Bitmap output, Bitmap source, boolean enableAntiAliasing) {
    Preconditions.checkNotNull(source);
    Preconditions.checkNotNull(output);
    output.setHasAlpha(true);
    Paint circlePaint;
    Paint xfermodePaint;
    if (enableAntiAliasing) {
      circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      xfermodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    } else {
      circlePaint = new Paint();
      xfermodePaint = new Paint();
    }
    circlePaint.setColor(Color.BLACK);
    xfermodePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

    Canvas canvas = new Canvas(output);
    float xCenter = source.getWidth() / 2f;
    float yCenter = source.getHeight() / 2f;
    canvas.drawCircle(xCenter, yCenter, Math.min(xCenter, yCenter), circlePaint);
    canvas.drawBitmap(source, 0, 0, xfermodePaint);
  }

  public static boolean canUseXferRoundFilter() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
  }
}
