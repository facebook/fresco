/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class BaseRepeatedPostProcessor extends BasePostprocessor
    implements RepeatedPostprocessor {
  @Nullable private RepeatedPostprocessorRunner mCallback;

  @Override
  public synchronized void setCallback(RepeatedPostprocessorRunner runner) {
    mCallback = runner;
  }

  @Nullable
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
