/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.internal;

import android.os.Process;
import androidx.annotation.Nullable;
import com.facebook.imagepipeline.core.ExecutorSupplier;
import com.facebook.imagepipeline.core.PriorityThreadFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/** An ExecutorSupplier we use just for ScrollPerf */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ScrollPerfExecutorSupplier implements ExecutorSupplier {

  // Allows for simultaneous reads and writes.
  private static final int NUM_IO_BOUND_THREADS = 2;
  private static final int NUM_LIGHTWEIGHT_BACKGROUND_THREADS = 1;

  private final Executor mIoBoundExecutor;
  private final Executor mDecodeExecutor;
  private final Executor mBackgroundExecutor;
  private final Executor mLightWeightBackgroundExecutor;

  public ScrollPerfExecutorSupplier(int numCpuBoundThreads, int numDecodingThread) {
    ThreadFactory backgroundPriorityThreadFactory =
        new PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND);

    mIoBoundExecutor = Executors.newFixedThreadPool(NUM_IO_BOUND_THREADS);
    mDecodeExecutor =
        Executors.newFixedThreadPool(numDecodingThread, backgroundPriorityThreadFactory);
    mBackgroundExecutor =
        Executors.newFixedThreadPool(numCpuBoundThreads, backgroundPriorityThreadFactory);
    mLightWeightBackgroundExecutor =
        Executors.newFixedThreadPool(
            NUM_LIGHTWEIGHT_BACKGROUND_THREADS, backgroundPriorityThreadFactory);
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

  @Nullable
  @Override
  public ScheduledExecutorService scheduledExecutorServiceForBackgroundTasks() {
    return null;
  }

  @Override
  public Executor forLightweightBackgroundTasks() {
    return mLightWeightBackgroundExecutor;
  }

  @Override
  public Executor forThumbnailProducer() {
    return mIoBoundExecutor;
  }
}
