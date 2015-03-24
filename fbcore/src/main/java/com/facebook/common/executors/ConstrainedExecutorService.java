/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.executors;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.facebook.common.logging.FLog;


/**
 * A {@link java.util.concurrent.ExecutorService} that delegates to an existing {@link Executor}
 * but constrains the number of concurrently executing tasks to a pre-configured value.
 */
public class ConstrainedExecutorService extends AbstractExecutorService {

  private static final Class<?> TAG = ConstrainedExecutorService.class;

  private final String mName;
  private final Executor mExecutor;
  private volatile int mMaxConcurrency;
  private final BlockingQueue<Runnable> mWorkQueue;

  private final Worker mTaskRunner;
  private final AtomicInteger mPendingWorkers;
  private final AtomicInteger mMaxQueueSize;

  /**
   * Creates a new {@code ConstrainedExecutorService}.
   * @param name Friendly name to identify the executor in logging and reporting.
   * @param maxConcurrency Maximum number of tasks to execute in parallel on the delegate executor.
   * @param executor Delegate executor for actually running tasks.
   * @param workQueue Queue to hold {@link Runnable}s for eventual execution.
   */
  public ConstrainedExecutorService(
      String name,
      int maxConcurrency,
      Executor executor,
      BlockingQueue<Runnable> workQueue) {
    if (maxConcurrency <= 0) {
      throw new IllegalArgumentException("max concurrency must be > 0");
    }
    mName = name;
    mExecutor = executor;
    mMaxConcurrency = maxConcurrency;
    mWorkQueue = workQueue;
    mTaskRunner = new Worker();
    mPendingWorkers = new AtomicInteger(0);
    mMaxQueueSize = new AtomicInteger(0);
  }

  /**
   * Factory method to create a new {@code ConstrainedExecutorService} with an unbounded
   * {@link LinkedBlockingQueue} queue.
   * @param name Friendly name to identify the executor in logging and reporting.
   * @param maxConcurrency Maximum number of tasks to execute in parallel on the delegate executor.
   * @param queueSize Number of items that can be queued before new submissions are rejected.
   * @param executor Delegate executor for actually running tasks.
   * @return new {@code ConstrainedExecutorService} instance.
   */
  public static ConstrainedExecutorService newConstrainedExecutor(
      String name,
      int maxConcurrency,
      int queueSize,
      Executor executor) {
    return new ConstrainedExecutorService(
        name,
        maxConcurrency,
        executor,
        new LinkedBlockingQueue<Runnable>(queueSize));
  }

  /**
   * Determine whether or not the queue is idle.
   * @return true if there is no work being executed and the work queue is empty, false otherwise.
   */
  public boolean isIdle() {
    return mWorkQueue.isEmpty() && (mPendingWorkers.get() == 0);
  }

  /**
   * Submit a task to be executed in the future.
   * @param runnable The task to be executed.
   */
  @Override
  public void execute(Runnable runnable) {
    if (runnable == null) {
      throw new NullPointerException("runnable parameter is null");
    }

    if (!mWorkQueue.offer(runnable)) {
      throw new RejectedExecutionException(
          mName + " queue is full, size=" + mWorkQueue.size());
    }

    final int queueSize = mWorkQueue.size();
    final int maxSize = mMaxQueueSize.get();
    if ((queueSize > maxSize) && mMaxQueueSize.compareAndSet(maxSize, queueSize)) {
      FLog.v(TAG, "%s: max pending work in queue = %d", mName, queueSize);
    } // else, there was a race and another thread updated and logged the max queue size

    startWorkerIfNeeded();
  }

  /**
   * Submits the single {@code Worker} instance {@code mTaskRunner} to the underlying executor an
   * additional time if there are fewer than {@code mMaxConcurrency} pending submissions. Does
   * nothing if the maximum number of workers is already pending.
   */
  private void startWorkerIfNeeded() {
    // Perform a compare-and-swap retry loop for synchronization to make sure we don't start more
    // workers than desired.
    int currentCount = mPendingWorkers.get();
    while (currentCount < mMaxConcurrency) {
      int updatedCount = currentCount + 1;
      if (mPendingWorkers.compareAndSet(currentCount, updatedCount)) {
        // Start a new worker.
        FLog.v(TAG, "%s: starting worker %d of %d", mName, updatedCount, mMaxConcurrency);
        mExecutor.execute(mTaskRunner);
        break;
      }
      // else: compareAndSet failed due to race; snapshot the new count and try again
      FLog.v(TAG, "%s: race in startWorkerIfNeeded; retrying", mName);
      currentCount = mPendingWorkers.get();
    }
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

  /**
   * Private worker class that removes one task from the work queue and runs it. This class
   * maintains no state of its own, so a single instance may be submitted to an executor
   * multiple times.
   */
  private class Worker implements Runnable {

    @Override
    public void run() {
      try {
        Runnable runnable = mWorkQueue.poll();
        if (runnable != null) {
          runnable.run();
        } else {
          // It is possible that a new worker was started before a previously started worker
          // de-queued its work item.
          FLog.v(TAG, "%s: Worker has nothing to run", mName);
        }
      } finally {
        int workers = mPendingWorkers.decrementAndGet();
        if (!mWorkQueue.isEmpty()) {
          startWorkerIfNeeded();
        } else {
          FLog.v(TAG, "%s: worker finished; %d workers left", mName, workers);
        }
      }
    }
  }
}
