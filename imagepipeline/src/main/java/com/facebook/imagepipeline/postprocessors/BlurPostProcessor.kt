/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.postprocessors

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.ScriptIntrinsicBlur
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.internal.Preconditions
import com.facebook.imagepipeline.filter.IterativeBoxBlurFilter
import com.facebook.imagepipeline.filter.RenderScriptBlurFilter
import com.facebook.imagepipeline.request.BasePostprocessor
import java.util.Locale

/**
 * A java implementation of a blur post processor. This provide two different blurring algorithm,
 * one Gaussian blur using [ScriptIntrinsicBlur] for Android version >= 4.2 and the other one is an
 * in-place iterative box blur algorithm that runs faster than a traditional box blur.
 */
class BlurPostProcessor
/**
 * Creates an instance of [BlurPostProcessor].
 *
 * @param blurRadius The radius of the blur in range 0 < radius <= [ ]
 *   [RenderScriptBlurFilter.BLUR_MAX_RADIUS].
 * @param context A valid [Context].
 * @param iterations The number of iterations of the blurring algorithm > 0.
 */
@JvmOverloads
constructor(val blurRadius: Int, val context: Context, val iterations: Int = DEFAULT_ITERATIONS) :
    BasePostprocessor() {

  init {
    Preconditions.checkArgument(
        blurRadius > 0 && blurRadius <= RenderScriptBlurFilter.BLUR_MAX_RADIUS)
    Preconditions.checkArgument(iterations > 0)
  }

  private val cacheKey: CacheKey =
      SimpleCacheKey(
          if (canUseRenderScript) {
            String.format(null as Locale?, "IntrinsicBlur;%d", blurRadius)
          } else {
            String.format(null as Locale?, "IterativeBoxBlur;%d;%d", iterations, blurRadius)
          })

  override fun getPostprocessorCacheKey(): CacheKey = cacheKey

  override fun process(destBitmap: Bitmap, sourceBitmap: Bitmap) {
    if (canUseRenderScript) {
      RenderScriptBlurFilter.blurBitmap(destBitmap, sourceBitmap, context, blurRadius)
    } else {
      super.process(destBitmap, sourceBitmap)
    }
  }

  override fun process(bitmap: Bitmap) {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(bitmap, iterations, blurRadius)
  }

  companion object {
    private val canUseRenderScript = RenderScriptBlurFilter.canUseRenderScript()
    private const val DEFAULT_ITERATIONS = 3
  }
}
