/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.postprocessor;

import android.graphics.Bitmap;
import com.facebook.cache.common.CacheKey;
import com.facebook.fresco.samples.showcase.imagepipeline.DurationCallback;
import com.facebook.imagepipeline.request.BasePostprocessor;
import javax.annotation.Nullable;

/** Postprocessor that measures the performance of {@link BasePostprocessor#process(Bitmap)}. */
public class BenchmarkPostprocessorForDuplicatedBitmapInPlace
    extends BasePostprocessorWithDurationCallback {

  private final BasePostprocessor mPostprocessor;

  public BenchmarkPostprocessorForDuplicatedBitmapInPlace(
      DurationCallback durationCallback, BasePostprocessor postprocessor) {
    super(durationCallback);
    mPostprocessor = postprocessor;
  }

  @Override
  public void process(Bitmap bitmap) {
    long startTime = System.nanoTime();
    mPostprocessor.process(bitmap);
    showDuration(System.nanoTime() - startTime);
  }

  @Nullable
  @Override
  public CacheKey getPostprocessorCacheKey() {
    return mPostprocessor.getPostprocessorCacheKey();
  }
}
