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
import com.facebook.imagepipeline.nativecode.NativeRoundingFilter
import com.facebook.imagepipeline.request.BasePostprocessor
import kotlin.math.min

class RoundedCornersPostprocessor : BasePostprocessor() {

  private var cacheKey: CacheKey? = null

  override fun process(bitmap: Bitmap) {
    val radius = min(bitmap.height, bitmap.width)
    NativeRoundingFilter.addRoundedCorners(bitmap, radius / 2, radius / 3, radius / 4, radius / 5)
  }

  override fun getPostprocessorCacheKey(): CacheKey? {
    if (cacheKey == null) {
      cacheKey = SimpleCacheKey("RoundedCornersPostprocessor")
    }
    return cacheKey
  }
}
