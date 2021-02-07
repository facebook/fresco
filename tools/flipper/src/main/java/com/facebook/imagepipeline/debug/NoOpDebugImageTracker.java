/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;

/** No-op debug image tracker. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class NoOpDebugImageTracker implements DebugImageTracker {

  @Override
  public void trackImage(ImageRequest imageRequest, CacheKey cacheKey) {
    // no-op
  }

  @Override
  public void trackImageRequest(ImageRequest imageRequest, String requestId) {
    // no-op
  }
}
