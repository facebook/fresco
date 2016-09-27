/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.postprocessors;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.nativecode.NativeBlurFilter;
import com.facebook.imagepipeline.request.BasePostprocessor;

/**
 * A fast and memory-efficient post processor performing an iterative box blur.  For details see
 * {@link NativeBlurFilter#iterativeBoxBlur(Bitmap, int, int)}.
 */
public class IterativeBoxBlurPostProcessor extends BasePostprocessor {

  private static final int DEFAULT_ITERATIONS = 3;

  final int mIterations;
  final int mBlurRadius;

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
}
