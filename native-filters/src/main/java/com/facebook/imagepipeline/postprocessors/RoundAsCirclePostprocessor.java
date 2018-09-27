/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.postprocessors;

import android.graphics.Bitmap;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.imagepipeline.nativecode.NativeRoundingFilter;
import com.facebook.imagepipeline.request.BasePostprocessor;
import javax.annotation.Nullable;

/** Postprocessor that rounds a given image as a circle. */
public class RoundAsCirclePostprocessor extends BasePostprocessor {
  private static final boolean ENABLE_ANTI_ALIASING = true;

  private @Nullable CacheKey mCacheKey;
  private final boolean mEnableAntiAliasing;

  public RoundAsCirclePostprocessor() {
    this(ENABLE_ANTI_ALIASING);
  }

  public RoundAsCirclePostprocessor(boolean enableAntiAliasing) {
    mEnableAntiAliasing = enableAntiAliasing;
  }

  @Override
  public void process(Bitmap bitmap) {
    NativeRoundingFilter.toCircle(bitmap, mEnableAntiAliasing);
  }

  @Nullable
  @Override
  public CacheKey getPostprocessorCacheKey() {
    if (mCacheKey == null) {
      if (mEnableAntiAliasing) {
        mCacheKey = new SimpleCacheKey("RoundAsCirclePostprocessor#AntiAliased");
      } else {
        mCacheKey = new SimpleCacheKey("RoundAsCirclePostprocessor");
      }
    }
    return mCacheKey;
  }
}
