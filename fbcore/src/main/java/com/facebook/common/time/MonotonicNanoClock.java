/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.time;

import com.facebook.common.internal.DoNotStrip;
import javax.annotation.concurrent.ThreadSafe;

/** A clock that is guaranteed not to go backward. */
@ThreadSafe
public interface MonotonicNanoClock {

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
}
