/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

public class FlipperCacheKeyFactory extends DefaultCacheKeyFactory {

  private final @Nullable DebugImageTracker mDebugImageTracker;

  public FlipperCacheKeyFactory(@Nullable DebugImageTracker debugImageTracker) {
    mDebugImageTracker = debugImageTracker;
  }

  @Override
  public CacheKey getBitmapCacheKey(ImageRequest request, @Nullable Object callerContext) {
    CacheKey bitmapCacheKey = super.getBitmapCacheKey(request, callerContext);
    if (mDebugImageTracker != null) {
      mDebugImageTracker.trackImage(request, bitmapCacheKey);
    }
    return bitmapCacheKey;
  }
}
