/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.postprocessor;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import java.util.Locale;

/**
 * Adds a watermark at random positions to the bitmap like {@link WatermarkPostprocessor}. However,
 * this implementation specifies a cache key such that the bitmap stays the same every time the
 * postprocessor is applied.
 */
public class CachedWatermarkPostprocessor extends WatermarkPostprocessor {

  public CachedWatermarkPostprocessor(int count, String watermarkText) {
    super(count, watermarkText);
  }

  @Override
  public CacheKey getPostprocessorCacheKey() {
    return new SimpleCacheKey(
        String.format((Locale) null, "text=%s,count=%d", mWatermarkText, mCount));
  }
}
