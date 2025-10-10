/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import android.graphics.Bitmap
import com.facebook.common.internal.DoNotStrip

/** A fast native blur filter. See [NativeBlurFilter#iterativeBoxBlur] */
@DoNotStrip
object NativeBlurFilter {

  init {
    NativeFiltersLoader.load()
  }

  /**
   * This is a fast, native implementation of an iterative box blur. The algorithm runs in-place on
   * the provided bitmap and therefore has a very small memory footprint.
   *
   * The iterative box blur has the nice property that it approximates the Gaussian blur very
   * quickly. Usually iterations=3 is sufficient such that the casual observer cannot tell the
   * difference.
   *
   * The edge pixels are repeated such that the bitmap still has a well-defined border.
   *
   * Asymptotic runtime: O(width * height * iterations)
   *
   * Asymptotic memory: O(radius + max(width, height))
   *
   * @param bitmap The targeted bitmap that will be blurred in-place. Each dimension must not be
   *   greater than 65536.
   * @param iterations The number of iterations to run. Must be greater than 0 and not greater
   *   than 65536.
   * @param blurRadius The given blur radius. Must be greater than 0 and not greater than 65536.
   */
  @JvmStatic
  /**
   * This is a fast, native implementation of an iterative box blur. The algorithm runs in-place on
   * the provided bitmap and therefore has a very small memory footprint.
   *
   * The iterative box blur has the nice property that it approximates the Gaussian blur very
   * quickly. Usually iterations=3 is sufficient such that the casual observer cannot tell the
   * difference.
   *
   * The edge pixels are repeated such that the bitmap still has a well-defined border.
   *
   * Asymptotic runtime: O(width * height * iterations)
   *
   * Asymptotic memory: O(radius + max(width, height))
   *
   * @param bitmap The targeted bitmap that will be blurred in-place. Each dimension must not be
   *   greater than 65536.
   * @param iterations The number of iterations to run. Must be greater than 0 and not greater
   *   than 65536.
   * @param blurRadius The given blur radius. Must be greater than 0 and not greater than 65536.
   */
  fun iterativeBoxBlur(bitmap: Bitmap, iterations: Int, blurRadius: Int) {
    checkNotNull(bitmap)
    require(iterations > 0)
    require(blurRadius > 0)
    nativeIterativeBoxBlur(bitmap, iterations, blurRadius)
  }

  @JvmStatic
  @DoNotStrip
  private external fun nativeIterativeBoxBlur(bitmap: Bitmap, iterations: Int, blurRadius: Int)
}
