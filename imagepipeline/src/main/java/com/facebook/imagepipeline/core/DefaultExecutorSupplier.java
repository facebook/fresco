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

/**
 * Basic implementation of {@link ExecutorSupplier} that provides one thread pool for the
 * CPU-bound operations and another thread pool for the IO-bound operations.
 */
public class DefaultExecutorSupplier implements ExecutorSupplier {
  // Allows for simultaneous reads and writes.
  private static final int NUM_IO_BOUND_THREADS = 2;
  private static final int NUM_CPU_BOUND_THREADS = Runtime.getRuntime().availableProcessors();

  private final Executor mIoBoundExecutor;
  private final Executor mCpuBoundExecutor;

  public DefaultExecutorSupplier() {
    mIoBoundExecutor = Executors.newFixedThreadPool(NUM_IO_BOUND_THREADS);
    mCpuBoundExecutor = Executors.newFixedThreadPool(NUM_CPU_BOUND_THREADS);
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
    return mCpuBoundExecutor;
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
