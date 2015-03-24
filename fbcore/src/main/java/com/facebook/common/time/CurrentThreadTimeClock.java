/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
