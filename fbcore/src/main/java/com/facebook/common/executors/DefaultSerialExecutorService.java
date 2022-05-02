/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.executors;

import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Default implementation of {@link SerialExecutorService} that wraps an existing {@link Executor}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DefaultSerialExecutorService extends ConstrainedExecutorService
    implements SerialExecutorService {

  public DefaultSerialExecutorService(Executor executor) {
    // SerialExecutorService is just a ConstrainedExecutorService with a concurrency limit
    // of one and an unbounded work queue.
    super("SerialExecutor", 1, executor, new LinkedBlockingQueue<Runnable>());
  }

  /**
   * Synchronized override of {@link ConstrainedExecutorService#execute(Runnable)} to ensure that
   * view of memory is consistent between different threads executing tasks serially.
   *
   * @param runnable The task to be executed.
   */
  @Override
  public synchronized void execute(Runnable runnable) {
    super.execute(runnable);
  }
}
