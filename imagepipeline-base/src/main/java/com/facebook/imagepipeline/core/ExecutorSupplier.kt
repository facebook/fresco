/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService

/**
 * Implementations of this interface are responsible for supplying the different executors used by
 * different sections of the image pipeline.
 *
 * A very basic implementation would supply a single thread pool for all four operations. It is
 * recommended that [.forLocalStorageRead] and [.forLocalStorageWrite] at least be different, as
 * their threads will be I/O-bound, rather than CPU-bound as the others are.
 *
 * Implementations should return singleton objects from these methods.
 *
 * {@see Executor}
 */
interface ExecutorSupplier {
  /** Executor used to do all disk reads, whether for disk cache or local files. */
  fun forLocalStorageRead(): Executor

  /** Executor used to do all disk writes, whether for disk cache or local files. */
  fun forLocalStorageWrite(): Executor

  /** Executor used for all decodes. */
  fun forDecode(): Executor

  /**
   * Executor used for background tasks such as image transcoding, resizing, rotating and post
   * processing.
   */
  fun forBackgroundTasks(): Executor

  fun scheduledExecutorServiceForBackgroundTasks(): ScheduledExecutorService?

  /**
   * Executor used for lightweight background operations, such as handing request off the main
   * thread.
   */
  fun forLightweightBackgroundTasks(): Executor

  fun forThumbnailProducer(): Executor
}
