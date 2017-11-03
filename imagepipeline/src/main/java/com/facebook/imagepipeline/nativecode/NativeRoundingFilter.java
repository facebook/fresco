/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import android.graphics.Bitmap;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;

/** A fast native rounding filter. */
@DoNotStrip
public class NativeRoundingFilter {

  static {
    ImagePipelineNativeLoader.load();
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
  public static void toCircle(Bitmap bitmap) {
    Preconditions.checkNotNull(bitmap);
    nativeToCircleFilter(bitmap);
  }

  @DoNotStrip
  private static native void nativeToCircleFilter(Bitmap bitmap);
}
