/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import android.os.Process;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Basic implementation of {@link ExecutorSupplier}.
 *
 * <p> Provides one thread pool for the CPU-bound operations and another thread pool for the
 * IO-bound operations.
 */
public class DefaultExecutorSupplier implements ExecutorSupplier {
  // Allows for simultaneous reads and writes.
  private static final int NUM_IO_BOUND_THREADS = 2;
  private static final int NUM_LIGHTWEIGHT_BACKGROUND_THREADS = 1;

  private final Executor mIoBoundExecutor;
  private final Executor mDecodeExecutor;
  private final Executor mBackgroundExecutor;
  private final Executor mLightWeightBackgroundExecutor;

  public DefaultExecutorSupplier(int numCpuBoundThreads) {
    mIoBoundExecutor =
        Executors.newFixedThreadPool(
            NUM_IO_BOUND_THREADS,
            new PriorityThreadFactory(
                Process.THREAD_PRIORITY_BACKGROUND, "FrescoIoBoundExecutor", true));
    mDecodeExecutor =
        Executors.newFixedThreadPool(
            numCpuBoundThreads,
            new PriorityThreadFactory(
                Process.THREAD_PRIORITY_BACKGROUND, "FrescoDecodeExecutor", true));
    mBackgroundExecutor =
        Executors.newFixedThreadPool(
            numCpuBoundThreads,
            new PriorityThreadFactory(
                Process.THREAD_PRIORITY_BACKGROUND, "FrescoBackgroundExecutor", true));
    mLightWeightBackgroundExecutor =
        Executors.newFixedThreadPool(
            NUM_LIGHTWEIGHT_BACKGROUND_THREADS,
            new PriorityThreadFactory(
                Process.THREAD_PRIORITY_BACKGROUND, "FrescoLightWeightBackgroundExecutor", true));

  }

  @Override
  public Executor forLocalStorageRead() {
    return mIoBoundExecutor;
  }

  @Override
  public Executor forLocalStorageWrite() {
    return mIoBoundExecutor;
  }

  @Override
  public Executor forDecode() {
    return mDecodeExecutor;
  }

  @Override
  public Executor forBackgroundTasks() {
    return mBackgroundExecutor;
  }

  @Override
  public Executor forLightweightBackgroundTasks() {
    return mLightWeightBackgroundExecutor;
  }
}
