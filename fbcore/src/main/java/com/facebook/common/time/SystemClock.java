/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.time;

import com.facebook.infer.annotation.Nullsafe;

/** Implementation of {@link Clock} that delegates to the system clock. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class SystemClock implements Clock {

  private static final SystemClock INSTANCE = new SystemClock();

  private SystemClock() {}

  public static SystemClock get() {
    return INSTANCE;
  }

  @Override
  public long now() {
    return System.currentTimeMillis();
  }
}
