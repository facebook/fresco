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

/** Postprocessor that rounds a given image as a circle. */
class RoundAsCirclePostprocessor
@JvmOverloads
constructor(private val enableAntiAliasing: Boolean = ENABLE_ANTI_ALIASING) : BasePostprocessor() {

  private var cacheKey: CacheKey? = null

  override fun process(bitmap: Bitmap) {
    NativeRoundingFilter.toCircleFast(bitmap, enableAntiAliasing)
  }

  override fun getPostprocessorCacheKey(): CacheKey? {
    if (cacheKey == null) {
      cacheKey =
          if (enableAntiAliasing) {
            SimpleCacheKey("RoundAsCirclePostprocessor#AntiAliased")
          } else {
            SimpleCacheKey("RoundAsCirclePostprocessor")
          }
    }
    return cacheKey
  }

  companion object {
    private const val ENABLE_ANTI_ALIASING = true
  }
}
