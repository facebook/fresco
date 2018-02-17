/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
