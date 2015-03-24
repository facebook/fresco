/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.executors;

import javax.annotation.Nullable;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.os.Handler;

/**
 * A {@link HandlerExecutorService} implementation.
 */
public class HandlerExecutorServiceImpl extends AbstractExecutorService
    implements HandlerExecutorService {

  private final Handler mHandler;

  public HandlerExecutorServiceImpl(Handler handler) {
    mHandler = handler;
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void execute(Runnable command) {
    mHandler.post(command);
  }

  @Override
  protected <T> ScheduledFutureImpl<T> newTaskFor(Runnable runnable, T value) {
    return new ScheduledFutureImpl<T>(mHandler, runnable, value);
  }

  @Override
  protected <T> ScheduledFutureImpl<T> newTaskFor(Callable<T> callable) {
    return new ScheduledFutureImpl<T>(mHandler, callable);
  }

  @Override
  public ScheduledFuture<?> submit(Runnable task) {
    return submit(task, (Void) null);
  }

  @Override
  public <T> ScheduledFuture<T> submit(Runnable task, @Nullable T result) {
    if (task == null) throw new NullPointerException();
    ScheduledFutureImpl<T> future = newTaskFor(task, result);
    execute(future);
    return future;
  }

  @Override
  public <T> ScheduledFuture<T> submit(Callable<T> task) {
    if (task == null) throw new NullPointerException();
    ScheduledFutureImpl<T> future = newTaskFor(task);
    execute(future);
    return future;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    ScheduledFutureImpl<?> future = newTaskFor(command, null);
    mHandler.postDelayed(future, unit.toMillis(delay));
    return future;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    ScheduledFutureImpl<V> future = newTaskFor(callable);
    mHandler.postDelayed(future, unit.toMillis(delay));
    return future;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(
      Runnable command, long initialDelay, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void quit() {
    mHandler.getLooper().quit();
  }

  @Override
  public boolean isHandlerThread() {
    return Thread.currentThread() == mHandler.getLooper().getThread();
  }
}
