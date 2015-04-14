/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
