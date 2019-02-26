/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import androidx.annotation.RequiresApi;
import com.facebook.common.internal.Preconditions;

public abstract class RenderScriptBlurFilter {

  public static final int BLUR_MAX_RADIUS = 25;

  /**
   * Not-in-place intrinsic Gaussian blur filter using {@link ScriptIntrinsicBlur} and {@link
   * RenderScript}. This require an Android versions >= 4.2.
   *
   * @param dest The {@link Bitmap} where the blurred image is written to.
   * @param src The {@link Bitmap} containing the original image.
   * @param context The {@link Context} necessary to use {@link RenderScript}
   * @param radius The radius of the blur with a supported range 0 < radius <= {@link
   *     #BLUR_MAX_RADIUS}
   */
  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  public static void blurBitmap(
      final Bitmap dest, final Bitmap src, final Context context, final int radius) {
    Preconditions.checkNotNull(dest);
    Preconditions.checkNotNull(src);
    Preconditions.checkNotNull(context);
    Preconditions.checkArgument(radius > 0 && radius <= BLUR_MAX_RADIUS);
    RenderScript rs = null;
    try {
      rs = RenderScript.create(context);

      // Create an Intrinsic Blur Script using the Renderscript
      ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

      // Create the input/output allocations with Renderscript and the src/dest bitmaps
      Allocation allIn = Allocation.createFromBitmap(rs, src);
      Allocation allOut = Allocation.createFromBitmap(rs, dest);

      // Set the radius of the blur
      blurScript.setRadius(radius);
      blurScript.setInput(allIn);
      blurScript.forEach(allOut);
      allOut.copyTo(dest);
    } finally {
      if (rs != null) {
        rs.destroy();
      }
    }
  }

  public static boolean canUseRenderScript() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
  }
}
