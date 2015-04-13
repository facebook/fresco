/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.facebook.common.executors.SerialDelegatingExecutor;

/**
 * Basic implementation of {@link ExecutorSupplier}.
 *
 * <p> Provides one thread pool for the CPU-bound operations and another thread pool for the
 * IO-bound operations. Decoding, a CPU-intensive operation, is limited to one thread.
 */
public class DefaultExecutorSupplier implements ExecutorSupplier {
  // Allows for simultaneous reads and writes.
  private static final int NUM_IO_BOUND_THREADS = 2;
  private static final int NUM_CPU_BOUND_THREADS = Runtime.getRuntime().availableProcessors();
  private static final int KEEP_ALIVE_SECONDS = 60;

  private final Executor mIoBoundExecutor;
  private final Executor mCpuBoundExecutor;
  private final Executor mDecodeExecutor;

  public DefaultExecutorSupplier() {
    mIoBoundExecutor = Executors.newFixedThreadPool(NUM_IO_BOUND_THREADS);
    mCpuBoundExecutor = new ThreadPoolExecutor(
        1,                     // keep at least that many threads alive
        NUM_CPU_BOUND_THREADS, // maximum number of allowed threads
        KEEP_ALIVE_SECONDS,    // amount of seconds each cached thread waits before being terminated
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>());
    mDecodeExecutor = new SerialDelegatingExecutor(mCpuBoundExecutor);
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
  public Executor forTransform() {
    return mCpuBoundExecutor;
  }

  @Override
  public Executor forBackground() {
    return mCpuBoundExecutor;
  }
}
