/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug

import com.facebook.cache.common.CacheKey
import com.facebook.imagepipeline.request.ImageRequest

/** No-op debug image tracker. */
class NoOpDebugImageTracker : DebugImageTracker {

  override fun trackImage(imageRequest: ImageRequest?, cacheKey: CacheKey) {
    // no-op
  }

  override fun trackImageRequest(imageRequest: ImageRequest?, requestId: String?) {
    // no-op
  }
}
