/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.executors;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An executor service that runs each task in the thread that invokes {@code execute/submit}.
 *
 * <p> This applies both to individually submitted tasks and to collections of tasks submitted via
 * {@code invokeAll} or {@code invokeAny}. In the latter case, tasks will run serially on the
 * calling thread. Tasks are run to completion before a {@code Future} is returned to the caller.
 *
 * <p> The implementation deviates from the {@code ExecutorService} specification with regards to
 * the {@code shutdownNow} and {@code awaitTermination} methods.
 * 1. A call to {@code shutdown} or {@code shutdownNow} is a no-op. A call to {@code isTerminated}
 *    always returns false.
 * 2. A call to {@code awaitTermination} always returns true immediately. True is returned in order
 *    to avoid potential infinite loop in the clients.
 * 3. "best-effort" with regards to canceling running tasks is implemented as "no-effort".
 *    No interrupts or other attempts are made to stop threads executing tasks.
 * 4. The returned list will always be empty, as any submitted task is considered to have started
 *    execution. This applies also to tasks given to {@code invokeAll} or {@code invokeAny} which
 *    are pending serial execution, including the tasks that have not yet started execution.
 */
public class CallerThreadExecutor extends AbstractExecutorService {

  private static final CallerThreadExecutor sInstance = new CallerThreadExecutor();

  public static CallerThreadExecutor getInstance() {
    return sInstance;
  }

  private CallerThreadExecutor() {
  }

  @Override
  public void execute(Runnable command) {
    command.run();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public void shutdown() {
    // no-op
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();
    return Collections.emptyList();
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return true;
  }
}
