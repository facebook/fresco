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
 * Implementation of {@link Clock} that delegates to the system clock.
 */
public class SystemClock implements Clock {

  private static final SystemClock INSTANCE = new SystemClock();

  private SystemClock() {
  }

  public static SystemClock get() {
    return INSTANCE;
  }

  @Override
  public long now() {
    return System.currentTimeMillis();
  }
}
