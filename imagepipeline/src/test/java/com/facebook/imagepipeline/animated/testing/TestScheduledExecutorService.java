/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.testing;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.facebook.common.testing.FakeClock;

public class TestScheduledExecutorService extends TestExecutorService
    implements ScheduledExecutorService {

  public TestScheduledExecutorService(FakeClock fakeClock) {
    super(fakeClock);
  }

  @Override
  public ScheduledFuture<?> schedule(final Runnable runnable, long delay, TimeUnit timeUnit) {
    return new TestScheduledFuture(
        getFakeClock(),
        scheduledQueue,
        TimeUnit.MILLISECONDS.convert(delay, timeUnit),
        runnable);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit timeUnit) {
    return new TestScheduledFuture<V>(
        getFakeClock(),
        scheduledQueue,
        TimeUnit.MILLISECONDS.convert(delay, timeUnit),
        callable);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable runnable,
      long initialDelay,
      long period,
      TimeUnit timeUnit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(
      Runnable runnable,
      long initialDelay,
      long delay,
      TimeUnit timeUnit) {
    throw new UnsupportedOperationException();
  }
}
