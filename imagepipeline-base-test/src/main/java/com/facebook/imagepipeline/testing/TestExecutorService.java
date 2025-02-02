/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.common.executors.SerialExecutorService;
import com.facebook.infer.annotation.Nullsafe;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/** Implementation of {@link java.util.concurrent.ExecutorService} for unit tests. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class TestExecutorService extends AbstractExecutorService implements SerialExecutorService {

  protected final ScheduledQueue scheduledQueue;
  private final FakeClock fakeClock;

  public TestExecutorService(FakeClock fakeClock) {
    this.fakeClock = fakeClock;
    this.scheduledQueue = new ScheduledQueue(fakeClock);
  }

  protected FakeClock getFakeClock() {
    return fakeClock;
  }

  /**
   * Gets the underlying queue that backs the executor service.
   *
   * @return the underlying queue
   */
  public ScheduledQueue getScheduledQueue() {
    return scheduledQueue;
  }

  @Override
  public void shutdown() {}

  @Nullable
  @Override
  // NULLSAFE_FIXME[Inconsistent Subclass Return Annotation]
  public List<Runnable> shutdownNow() {
    return null;
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
  public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
    throw new RuntimeException();
  }

  @Override
  public void execute(Runnable runnable) {
    scheduledQueue.add(runnable);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return super.newTaskFor(runnable, value);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return super.newTaskFor(callable);
  }

  public int getPendingCount() {
    return scheduledQueue.getPendingCount();
  }

  public void runUntilIdle() {
    scheduledQueue.runUntilIdle();
  }

  public void runNextPendingCommand() {
    scheduledQueue.runNextPendingCommand();
  }

  public boolean isIdle() {
    return scheduledQueue.isIdle();
  }
}
