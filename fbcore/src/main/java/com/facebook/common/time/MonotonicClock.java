/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.time;

import com.facebook.common.internal.DoNotStrip;

/**
 * A clock that is guaranteed not to go backward.
 */
public interface MonotonicClock {

  /**
   * Produce a timestamp.  Values returned from this method may only be compared to other values
   * returned from this clock in this process.  They have no meaning outside of this process
   * and should not be written to disk.
   *
   * The difference between two timestamps is an interval, in milliseconds.
   *
   * @return A timestamp for the current time, in ms.
   */
  @DoNotStrip
  long now();
}
