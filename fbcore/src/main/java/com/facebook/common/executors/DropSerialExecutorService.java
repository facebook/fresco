/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.executors;

import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Default implementation of {@link SerialExecutorService} that wraps an existing {@link Executor}.
 */
public class DropSerialExecutorService extends ConstrainedExecutorService
    implements SerialExecutorService {

  public DropSerialExecutorService(Executor executor) {
    // SerialExecutorService is just a ConstrainedExecutorService with a concurrency limit
    // of one and an unbounded work queue.
    super("DropSerialExecutor", 1, executor, new PriorityBlockingQueue<Runnable>());
  }

  /**
   * Synchronized override of {@link ConstrainedExecutorService#execute(Runnable)} to
   * ensure that view of memory is consistent between different threads executing tasks serially.
   * @param runnable The task to be executed.
   */
  @Override
  public synchronized void execute(Runnable runnable) {
    super.execute(runnable);
  }
}
