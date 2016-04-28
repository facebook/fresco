// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.common.time;

import com.facebook.common.internal.DoNotStrip;

/**
 * A clock that returns number of milliseconds since boot. It guarantees that every next
 * call to now() will return a value that is not less that was returned from previous call to now().
 * This happens regardless system time changes, time zone changes, daylight saving changes etc.
 *
 * DO USE THIS CLOCK FOR PERFORMANCE MEASUREMENT.  IT STOPS TICKING WHILE THE DEVICE SLEEPS, THAT
 * IS, WHILE THE DEVICE CANNOT RUN THE CODE WE ARE PURPORTEDLY MEASURING.
 */
@DoNotStrip
public class AwakeTimeSinceBootClock implements MonotonicClock {
  @DoNotStrip
  private static final AwakeTimeSinceBootClock INSTANCE = new AwakeTimeSinceBootClock();

  private AwakeTimeSinceBootClock() {
  }

  /**
   * Returns a singleton instance of this clock.
   * @return singleton instance
   */
  @DoNotStrip
  public static AwakeTimeSinceBootClock get() {
    return INSTANCE;
  }

  @Override
  @DoNotStrip
  public long now() {
    // Guaranteed to be monotonic according to documentation.
    return android.os.SystemClock.uptimeMillis();
  }
}
