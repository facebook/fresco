/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import android.os.Process
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Basic implementation of [ExecutorSupplier].
 *
 * Provides one thread pool for the CPU-bound operations and another thread pool for the IO-bound
 * operations.
 */
class DefaultExecutorSupplier(numCpuBoundThreads: Int) : ExecutorSupplier {

  private val ioBoundExecutor: Executor =
      Executors.newFixedThreadPool(
          NUM_IO_BOUND_THREADS,
          PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND, "FrescoIoBoundExecutor", true))
  private val decodeExecutor: Executor =
      Executors.newFixedThreadPool(
          numCpuBoundThreads,
          PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND, "FrescoDecodeExecutor", true))
  private val backgroundExecutor: Executor =
      Executors.newFixedThreadPool(
          numCpuBoundThreads,
          PriorityThreadFactory(
              Process.THREAD_PRIORITY_BACKGROUND, "FrescoBackgroundExecutor", true))
  private val lightWeightBackgroundExecutor: Executor =
      Executors.newFixedThreadPool(
          NUM_LIGHTWEIGHT_BACKGROUND_THREADS,
          PriorityThreadFactory(
              Process.THREAD_PRIORITY_BACKGROUND, "FrescoLightWeightBackgroundExecutor", true))
  private val backgroundScheduledExecutorService: ScheduledExecutorService =
      Executors.newScheduledThreadPool(
          numCpuBoundThreads,
          PriorityThreadFactory(
              Process.THREAD_PRIORITY_BACKGROUND, "FrescoBackgroundExecutor", true))

  override fun forLocalStorageRead(): Executor = ioBoundExecutor

  override fun forLocalStorageWrite(): Executor = ioBoundExecutor

  override fun forDecode(): Executor = decodeExecutor

  override fun forBackgroundTasks(): Executor = backgroundExecutor

  override fun scheduledExecutorServiceForBackgroundTasks(): ScheduledExecutorService? =
      backgroundScheduledExecutorService

  override fun forLightweightBackgroundTasks(): Executor = lightWeightBackgroundExecutor

  override fun forThumbnailProducer(): Executor = ioBoundExecutor

  companion object {
    // Allows for simultaneous reads and writes.
    private const val NUM_IO_BOUND_THREADS = 2
    private const val NUM_LIGHTWEIGHT_BACKGROUND_THREADS = 1
  }
}
