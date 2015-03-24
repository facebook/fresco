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
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.facebook.common.testing.FakeClock;

public class TestScheduledFuture<V> implements ScheduledFuture<V> {

  private final FakeClock mFakeClock;
  private final ScheduledQueue mScheduledQueue;
  private final long mScheduledTime;
  private final Runnable mWrap;
  private boolean mIsCanceled;
  private boolean mIsDone;
  private V mResult;
  private Throwable mResultThrowable;

  TestScheduledFuture(
      FakeClock fakeClock,
      ScheduledQueue scheduledQueue,
      long delay,
      final Runnable runnable) {
    this(
        fakeClock,
        scheduledQueue,
        delay,
        Executors.<V>callable(runnable, null));
  }

  TestScheduledFuture(
      FakeClock fakeClock,
      ScheduledQueue scheduledQueue,
      long delay,
      final Callable<V> callable) {
    mFakeClock = fakeClock;
    mScheduledQueue = scheduledQueue;
    mScheduledTime = mFakeClock.now() + delay;
    mWrap = new Runnable() {
      @Override
      public void run() {
        try {
          mResult = callable.call();
        } catch (Throwable t) {
          mResultThrowable = t;
        }
        mIsDone = true;
      }
    };
    mScheduledQueue.add(mWrap, delay);
  }

  @Override
  public long getDelay(TimeUnit timeUnit) {
    return timeUnit.convert(mFakeClock.now() - mScheduledTime, TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed delayed) {
    long me = getDelay(TimeUnit.MILLISECONDS);
    long other = delayed.getDelay(TimeUnit.MILLISECONDS);
    if (me < other) {
      return -1;
    } if (me > other) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public boolean cancel(boolean b) {
    mIsCanceled = mScheduledQueue.remove(mWrap);
    return mIsCanceled;
  }

  @Override
  public boolean isCancelled() {
    return mIsCanceled;
  }

  @Override
  public boolean isDone() {
    return mIsDone;
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    if (!mIsDone) {
      throw new IllegalStateException("Not yet done. We don't support blocking in tests");
    }
    if (mResultThrowable != null) {
      throw new ExecutionException(mResultThrowable);
    } else {
      return mResult;
    }
  }

  @Override
  public V get(long l, TimeUnit timeUnit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return get();
  }
}
