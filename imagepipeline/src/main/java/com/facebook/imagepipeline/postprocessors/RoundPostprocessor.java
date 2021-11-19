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
import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.filter.XferRoundFilter;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Postprocessor that rounds a given image as a circle using non-native code. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class RoundPostprocessor extends BasePostprocessor {
  private static final boolean ENABLE_ANTI_ALIASING = true;

  private @Nullable CacheKey mCacheKey;
  private final boolean mEnableAntiAliasing;

  public RoundPostprocessor() {
    this(ENABLE_ANTI_ALIASING);
  }

  public RoundPostprocessor(boolean enableAntiAliasing) {
    mEnableAntiAliasing = enableAntiAliasing;
  }

  @Override
  public void process(Bitmap destBitmap, Bitmap sourceBitmap) {
    Preconditions.checkNotNull(destBitmap);
    Preconditions.checkNotNull(sourceBitmap);
    XferRoundFilter.xferRoundBitmap(destBitmap, sourceBitmap, mEnableAntiAliasing);
  }

  @Nullable
  @Override
  public CacheKey getPostprocessorCacheKey() {
    if (mCacheKey == null) {
      mCacheKey = new SimpleCacheKey("XferRoundFilter");
    }
    return mCacheKey;
  }
}
