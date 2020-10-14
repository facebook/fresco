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
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class RoundedCornersPostprocessor extends BasePostprocessor {

  private @Nullable CacheKey mCacheKey;

  @Override
  public void process(Bitmap bitmap) {
    int radius = Math.min(bitmap.getHeight(), bitmap.getWidth());
    NativeRoundingFilter.addRoundedCorners(bitmap, radius / 2, radius / 3, radius / 4, radius / 5);
  }

  @Nullable
  @Override
  public CacheKey getPostprocessorCacheKey() {
    if (mCacheKey == null) {
      mCacheKey = new SimpleCacheKey("RoundedCornersPostprocessor");
    }
    return mCacheKey;
  }
}
