/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.activitylistener;

import android.app.Activity;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class BaseActivityListener implements ActivityListener {

  @Override
  public void onActivityCreate(Activity activity) {}

  @Override
  public void onStop(Activity activity) {}

  @Override
  public void onStart(Activity activity) {}

  @Override
  public void onDestroy(Activity activity) {}

  @Override
  public int getPriority() {
    return ActivityListener.MIN_PRIORITY;
  }

  @Override
  public void onPause(Activity activity) {}

  @Override
  public void onResume(Activity activity) {}
}
