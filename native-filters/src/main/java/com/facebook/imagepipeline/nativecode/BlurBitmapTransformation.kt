/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import android.graphics.Bitmap
import com.facebook.imagepipeline.transformation.BitmapTransformation

/**
 * A [BitmapTransformation] that applies a fast native iterative box blur in-place on the decoded
 * bitmap. Uses the native C++ blur filter via [NativeBlurFilter] for maximum efficiency.
 *
 * This is intended for use as an intermediate image transformation (e.g., blurring progressive JPEG
 * preview scans) via [ImageDecodeOptions.intermediateImageBitmapTransformation].
 */
class BlurBitmapTransformation(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val blurRadius: Int = DEFAULT_BLUR_RADIUS,
) : BitmapTransformation {

  init {
    require(iterations > 0) { "iterations must be > 0, got $iterations" }
    require(blurRadius > 0) { "blurRadius must be > 0, got $blurRadius" }
  }

  override fun transform(bitmap: Bitmap) {
    NativeBlurFilter.iterativeBoxBlur(bitmap, iterations, blurRadius)
  }

  override fun modifiesTransparency(): Boolean = false

  companion object {
    private const val DEFAULT_ITERATIONS = 3
    private const val DEFAULT_BLUR_RADIUS = 25
  }
}
