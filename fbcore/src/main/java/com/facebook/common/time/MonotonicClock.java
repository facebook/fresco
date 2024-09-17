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
import javax.annotation.concurrent.ThreadSafe;

/** A clock that is guaranteed not to go backward. */
@Nullsafe(Nullsafe.Mode.LOCAL)
@ThreadSafe
public interface MonotonicClock {

  /**
   * Produce a timestamp. Values returned from this method may only be compared to other values
   * returned from this clock in this process. They have no meaning outside of this process and
   * should not be written to disk.
   *
   * <p>The difference between two timestamps is an interval, in nanoseconds.
   *
   * @return A timestamp for the current time, in nanoseconds.
   */
  @DoNotStrip
  long nowNanos();

  /**
   * Produce a timestamp. Values returned from this method may only be compared to other values
   * returned from this clock in this process. They have no meaning outside of this process and
   * should not be written to disk. This method uses the same timesource as {@link #nowNanos()}
   *
   * <p>The difference between two timestamps is an interval, in milliseconds.
   *
   * @return A timestamp for the current time, in milliseconds.
   */
  @DoNotStrip
  default long now() {
    return TimeUnit.NANOSECONDS.toMillis(nowNanos());
  }

  /**
   * Produce a non-singleton MonotonicClock instance wrapping the given Clock.
   *
   * @return A MonotonicClock that uses the provided Clock.
   */
  static MonotonicClock of(Clock provider) {
    return new MonotonicClockWrapper(provider);
  }

  public static final class MonotonicClockWrapper implements MonotonicClock {
    private final Clock provider;
    private long mLast;

    private MonotonicClockWrapper(Clock provider) {
      this.provider = provider;
      mLast = provider.now();
    }

    @Override
    public long nowNanos() {
      return TimeUnit.MILLISECONDS.toNanos(now());
    }

    @Override
    public long now() {
      mLast = Math.max(mLast, provider.now());
      return mLast;
    }
  }
}
