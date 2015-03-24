/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.executors;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Default implementation of {@link SerialExecutorService} that wraps an existing {@link Executor}.
 */
public class DefaultSerialExecutorService extends ConstrainedExecutorService
    implements SerialExecutorService {

  public DefaultSerialExecutorService(Executor executor) {
    // SerialExecutorService is just a ConstrainedExecutorService with a concurrency limit
    // of one and an unbounded work queue.
    super("SerialExecutor", 1, executor, new LinkedBlockingQueue<Runnable>());
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
