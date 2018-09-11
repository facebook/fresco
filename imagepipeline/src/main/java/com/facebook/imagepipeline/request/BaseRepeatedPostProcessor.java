/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

public abstract class BaseRepeatedPostProcessor extends BasePostprocessor
    implements RepeatedPostprocessor {
  private RepeatedPostprocessorRunner mCallback;

  @Override
  public synchronized void setCallback(RepeatedPostprocessorRunner runner) {
    mCallback = runner;
  }

  private synchronized RepeatedPostprocessorRunner getCallback() {
    return mCallback;
  }

  public void update() {
    RepeatedPostprocessorRunner callback = getCallback();
    if (callback != null) {
      callback.update();
    }
  }
}
