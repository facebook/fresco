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

/**
 * Implementations of this interface are responsible for supplying the different executors
 * used by different sections of the image pipeline.
 *
 * <p>A very basic implementation would supply a single thread pool for all four operations.
 * It is recommended that {@link #forLocalStorageRead} and {@link #forLocalStorageWrite} at least
 * be different, as their threads will be I/O-bound, rather than CPU-bound as the others are.
 *
 * <p>Implementations should return singleton objects from these methods.
 *
 * <p>{@see Executor}
 */
public interface ExecutorSupplier {

  /** Executor used to do all disk reads, whether for disk cache or local files. */
  Executor forLocalStorageRead();

  /** Executor used to do all disk writes, whether for disk cache or local files. */
  Executor forLocalStorageWrite();

  /** Executor used for all decodes. */
  Executor forDecode();

  /**
   *  Executor used for background tasks such as image transcoding, resizing, rotating and
   *  post processing.
   */
  Executor forBackgroundTasks();

  /**
   * Executor used for lightweight background operations, such as handing request off the
   * main thread.
   */
  Executor forLightweightBackgroundTasks();
}
