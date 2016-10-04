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

/**
 * A fast native blur filter. See {@link NativeBlurFilter#iterativeBoxBlur}
 */
@DoNotStrip
public class NativeBlurFilter {

  static {
    ImagePipelineNativeLoader.load();
  }

  /**
   * This is a fast, native implementation of an iterative box blur. The algorithm runs in-place on
   * the provided bitmap and therefore has a very small memory footprint.
   * <p>
   * The iterative box blur has the nice property that it approximates the Gaussian blur very
   * quickly. Usually iterations=3 is sufficient such that the casual observer cannot tell the
   * difference.
   * <p>
   * The edge pixels are repeated such that the bitmap still has a well-defined border.
   * <p>
   * Asymptotic runtime: O(width * height * iterations)
   * <p>
   * Asymptotic memory: O(radius + max(width, height))
   *
   * @param bitmap The targeted bitmap that will be blurred in-place
   * @param iterations The number of iterations to run. Must be greater than 0.
   * @param blurRadius The given blur radius. Must be greater than 0.
   */
  public static void iterativeBoxBlur(Bitmap bitmap, int iterations, int blurRadius) {
    Preconditions.checkNotNull(bitmap);
    Preconditions.checkArgument(iterations > 0);
    Preconditions.checkArgument(blurRadius > 0);

    nativeIterativeBoxBlur(bitmap, iterations, blurRadius);
  }

  @DoNotStrip
  private static native void nativeIterativeBoxBlur(
      Bitmap bitmap,
      int iterations,
      int blurRadius);
}
