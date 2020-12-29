/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;

/**
 * Implementations of this interface are responsible for supplying the different executors used by
 * different sections of the image pipeline.
 *
 * <p>A very basic implementation would supply a single thread pool for all four operations. It is
 * recommended that {@link #forLocalStorageRead} and {@link #forLocalStorageWrite} at least be
 * different, as their threads will be I/O-bound, rather than CPU-bound as the others are.
 *
 * <p>Implementations should return singleton objects from these methods.
 *
 * <p>{@see Executor}
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface ExecutorSupplier {

  /** Executor used to do all disk reads, whether for disk cache or local files. */
  Executor forLocalStorageRead();

  /** Executor used to do all disk writes, whether for disk cache or local files. */
  Executor forLocalStorageWrite();

  /** Executor used for all decodes. */
  Executor forDecode();

  /**
   * Executor used for background tasks such as image transcoding, resizing, rotating and post
   * processing.
   */
  Executor forBackgroundTasks();

  @Nullable
  ScheduledExecutorService scheduledExecutorServiceForBackgroundTasks();

  /**
   * Executor used for lightweight background operations, such as handing request off the main
   * thread.
   */
  Executor forLightweightBackgroundTasks();

  Executor forThumbnailProducer();
}
