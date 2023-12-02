/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.time;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.TimeUnit;

/**
 * A clock that returns number of milliseconds since boot. It guarantees that every next call to
 * now() will return a value that is not less that was returned from previous call to now(). This
 * happens regardless system time changes, time zone changes, daylight saving changes etc.
 *
 * <p>NOTE: For performance logging, consider using {@link AwakeTimeSinceBootClock} since it stops
 * ticking while the device sleeps.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@DoNotStrip
public class RealtimeSinceBootClock implements MonotonicClock {
  private static final RealtimeSinceBootClock INSTANCE = new RealtimeSinceBootClock();

  private RealtimeSinceBootClock() {}

  /**
   * Returns a singleton instance of this clock.
   *
   * @return singleton instance
   */
  @DoNotStrip
  public static RealtimeSinceBootClock get() {
    return INSTANCE;
  }

  @Override
  public long now() {
    // Guaranteed to be monotonic according to documentation.
    return android.os.SystemClock.elapsedRealtime();
  }

  @Override
  public long nowNanos() {
    // Guaranteed to be monotonic according to documentation.
    return TimeUnit.MILLISECONDS.toNanos(now());
  }
}
