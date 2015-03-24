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
 * A clock that returns number of milliseconds since boot. It guarantees that every next
 * call to now() will return a value that is not less that was returned from previous call to now().
 * This happens regardless system time changes, time zone changes, daylight saving changes etc.
 */
public class RealtimeSinceBootClock implements MonotonicClock {
  private static final RealtimeSinceBootClock INSTANCE = new RealtimeSinceBootClock();

  private RealtimeSinceBootClock() {
  }

  /**
   * Returns a singleton instance of this clock.
   * @return singleton instance
   */
  public static RealtimeSinceBootClock get() {
    return INSTANCE;
  }

  @Override
  public long now() {
    // Guaranteed to be monotonic according to documentation.
    return android.os.SystemClock.elapsedRealtime();
  }
}
