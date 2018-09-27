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
import com.facebook.imagepipeline.nativecode.NativeBlurFilter;
import com.facebook.imagepipeline.request.BasePostprocessor;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * A fast and memory-efficient post processor performing an iterative box blur.  For details see
 * {@link NativeBlurFilter#iterativeBoxBlur(Bitmap, int, int)}.
 */
public class IterativeBoxBlurPostProcessor extends BasePostprocessor {

  private static final int DEFAULT_ITERATIONS = 3;

  private final int mIterations;
  private final int mBlurRadius;

  private CacheKey mCacheKey;

  public IterativeBoxBlurPostProcessor(int blurRadius) {
    this(DEFAULT_ITERATIONS, blurRadius);
  }

  public IterativeBoxBlurPostProcessor(int iterations, int blurRadius) {
    Preconditions.checkArgument(iterations > 0);
    Preconditions.checkArgument(blurRadius > 0);
    mIterations = iterations;
    mBlurRadius = blurRadius;
  }

  @Override
  public void process(Bitmap bitmap) {
    NativeBlurFilter.iterativeBoxBlur(bitmap, mIterations, mBlurRadius);
  }

  @Nullable
  @Override
  public CacheKey getPostprocessorCacheKey() {
    if (mCacheKey == null) {
      final String key = String.format((Locale) null, "i%dr%d", mIterations, mBlurRadius);
      mCacheKey = new SimpleCacheKey(key);
    }
    return mCacheKey;
  }
}
