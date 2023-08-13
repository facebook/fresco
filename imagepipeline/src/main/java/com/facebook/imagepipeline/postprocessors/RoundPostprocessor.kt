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
import com.facebook.imagepipeline.filter.XferRoundFilter
import com.facebook.imagepipeline.request.BasePostprocessor

/** Postprocessor that rounds a given image as a circle using non-native code. */
class RoundPostprocessor @JvmOverloads constructor(private val enableAntiAliasing: Boolean = true) :
    BasePostprocessor() {

  private val cacheKey: CacheKey = SimpleCacheKey("XferRoundFilter")

  override fun getPostprocessorCacheKey(): CacheKey = cacheKey

  override fun process(destBitmap: Bitmap, sourceBitmap: Bitmap) {
    XferRoundFilter.xferRoundBitmap(destBitmap, sourceBitmap, enableAntiAliasing)
  }
}
