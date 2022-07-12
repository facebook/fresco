/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug

import com.facebook.cache.common.CacheKey
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory
import com.facebook.imagepipeline.request.ImageRequest

class FlipperCacheKeyFactory(private val debugImageTracker: DebugImageTracker?) :
    DefaultCacheKeyFactory() {

  override fun getBitmapCacheKey(request: ImageRequest, callerContext: Any?): CacheKey {
    val bitmapCacheKey = super.getBitmapCacheKey(request, callerContext)
    debugImageTracker?.trackImage(request, bitmapCacheKey)
    return bitmapCacheKey
  }
}
