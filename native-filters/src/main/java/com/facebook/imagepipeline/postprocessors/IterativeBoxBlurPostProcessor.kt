/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.postprocessors

import android.graphics.Bitmap
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.imagepipeline.nativecode.NativeBlurFilter
import com.facebook.imagepipeline.request.BasePostprocessor
import java.util.Locale

/**
 * A fast and memory-efficient post processor performing an iterative box blur. For details see
 * [NativeBlurFilter#iterativeBoxBlur(Bitmap, int, int)].
 */
class IterativeBoxBlurPostProcessor(iterations: Int, blurRadius: Int) : BasePostprocessor() {

  private val iterations: Int
  private val blurRadius: Int

  private var cacheKey: CacheKey? = null

  init {
    require(iterations > 0)
    require(blurRadius > 0)
    this.iterations = iterations
    this.blurRadius = blurRadius
  }

  constructor(blurRadius: Int) : this(DEFAULT_ITERATIONS, blurRadius)

  override fun process(bitmap: Bitmap) {
    NativeBlurFilter.iterativeBoxBlur(bitmap, iterations, blurRadius)
  }

  override fun getPostprocessorCacheKey(): CacheKey? {
    if (cacheKey == null) {
      val key = String.format(null as Locale?, "i%dr%d", iterations, blurRadius)
      cacheKey = SimpleCacheKey(key)
    }
    return cacheKey
  }

  companion object {
    private const val DEFAULT_ITERATIONS = 3
  }
}
