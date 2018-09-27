/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import android.graphics.Bitmap;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;

/** A fast native rounding filter. */
@DoNotStrip
public class NativeRoundingFilter {

  static {
    NativeFiltersLoader.load();
  }

  public static void toCircle(Bitmap bitmap) {
    toCircle(bitmap, false);
  }

  /**
   * This is a fast, native implementation for rounding a bitmap. It takes the given bitmap and
   * modifies it to be circular.
   *
   * <p>This implementation does not change the underlying bitmap dimensions, it only sets pixels
   * that are outside of the circle to a transparent color.
   *
   * @param bitmap the bitmap to modify
   */
  public static void toCircle(Bitmap bitmap, boolean antiAliased) {
    Preconditions.checkNotNull(bitmap);
    nativeToCircleFilter(bitmap, antiAliased);
  }

  public static void toCircleWithBorder(
      Bitmap bitmap, int colorARGB, int borderWidthPx, boolean antiAliased) {
    Preconditions.checkNotNull(bitmap);
    nativeToCircleWithBorderFilter(bitmap, colorARGB, borderWidthPx, antiAliased);
  }

  @DoNotStrip
  private static native void nativeToCircleFilter(Bitmap bitmap, boolean antiAliased);

  @DoNotStrip
  private static native void nativeToCircleWithBorderFilter(
      Bitmap bitmap, int colorARGB, int borderWidthPx, boolean antiAliased);
}
