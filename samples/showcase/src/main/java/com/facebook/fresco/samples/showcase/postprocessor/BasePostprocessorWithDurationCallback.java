/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.postprocessor;

import com.facebook.fresco.samples.showcase.imagepipeline.DurationCallback;
import com.facebook.imagepipeline.request.BasePostprocessor;

/**
 * Simple extension of {@link BasePostprocessor} for this sample app that allows to pass along the
 * runtime of the post-processing.
 */
abstract class BasePostprocessorWithDurationCallback extends BasePostprocessor {

  private final DurationCallback mDurationCallback;

  BasePostprocessorWithDurationCallback(DurationCallback durationCallback) {
    mDurationCallback = durationCallback;
  }

  void showDuration(long startNs) {
    if (mDurationCallback != null) {
      mDurationCallback.showDuration(startNs);
    }
  }
}
