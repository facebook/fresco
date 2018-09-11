/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.postprocessors;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.ScriptIntrinsicBlur;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.filter.IterativeBoxBlurFilter;
import com.facebook.imagepipeline.filter.RenderScriptBlurFilter;
import com.facebook.imagepipeline.request.BasePostprocessor;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * A java implementation of a blur post processor. This provide two different blurring algorithm,
 * one Gaussian blur using {@link ScriptIntrinsicBlur} for Android version >= 4.2 and the other one
 * is an in-place iterative box blur algorithm that runs faster than a traditional box blur.
 */
public class BlurPostProcessor extends BasePostprocessor {

  private static final boolean canUseRenderScript = RenderScriptBlurFilter.canUseRenderScript();
  private static final int DEFAULT_ITERATIONS = 3;
  private final int mIterations;
  private final Context mContext;
  private final int mBlurRadius;
  private CacheKey mCacheKey;

  /**
   * Creates an instance of {@link BlurPostProcessor}.
   *
   * @param blurRadius The radius of the blur in range 0 < radius <= {@link
   *     RenderScriptBlurFilter#BLUR_MAX_RADIUS}.
   * @param context A valid {@link Context}.
   * @param iterations The number of iterations of the blurring algorithm > 0.
   */
  public BlurPostProcessor(final int blurRadius, final Context context, final int iterations) {
    Preconditions.checkArgument(
        blurRadius > 0 && blurRadius <= RenderScriptBlurFilter.BLUR_MAX_RADIUS);
    Preconditions.checkArgument(iterations > 0);
    Preconditions.checkNotNull(context);
    mIterations = iterations;
    mBlurRadius = blurRadius;
    mContext = context;
  }

  /**
   * Creates an instance of {@link BlurPostProcessor}.
   *
   * @param blurRadius The radius of the blur in range 0 < radius <= {@link
   *     RenderScriptBlurFilter#BLUR_MAX_RADIUS}.
   * @param context A valid {@link Context}.
   */
  public BlurPostProcessor(final int blurRadius, final Context context) {
    this(blurRadius, context, DEFAULT_ITERATIONS);
  }

  @Override
  public void process(final Bitmap destBitmap, final Bitmap sourceBitmap) {
    if (canUseRenderScript) {
      RenderScriptBlurFilter.blurBitmap(destBitmap, sourceBitmap, mContext, mBlurRadius);
    } else {
      super.process(destBitmap, sourceBitmap);
    }
  }

  @Override
  public void process(final Bitmap bitmap) {
    IterativeBoxBlurFilter.boxBlurBitmapInPlace(bitmap, mIterations, mBlurRadius);
  }

  @Nullable
  @Override
  public CacheKey getPostprocessorCacheKey() {
    if (mCacheKey == null) {
      final String key;
      if (canUseRenderScript) {
        key = String.format((Locale) null, "IntrinsicBlur;%d", mBlurRadius);
      } else {
        key = String.format((Locale) null, "IterativeBoxBlur;%d;%d", mIterations, mBlurRadius);
      }
      mCacheKey = new SimpleCacheKey(key);
    }
    return mCacheKey;
  }
}
