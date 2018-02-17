/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.time;

/**
 * A clock that returns milliseconds running in the current thread.
 * See {@link android.os.SystemClock}
 */
public class CurrentThreadTimeClock implements Clock {

  public CurrentThreadTimeClock() {}

  @Override
  public long now() {
    return android.os.SystemClock.currentThreadTimeMillis();
  }
}
