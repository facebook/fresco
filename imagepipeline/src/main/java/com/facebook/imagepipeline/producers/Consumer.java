/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

/**
 * Consumes data produced by {@link Producer}.<T>
 *
 * <p> The producer uses this interface to notify its client when new data is ready or an error
 * occurs. Execution of the image request is structured as a sequence of Producers. Each one
 * consumes data produced by producer preceding it in the sequence.
 *
 * <p>For example decode is a producer that consumes data produced by the disk cache get producer.
 *
 * <p> The consumer is passed new intermediate results via onNewResult(isLast = false) method. Each
 * consumer should expect that one of the following methods will be called exactly once, as the very
 * last producer call:
 * <ul>
 *   <li> onNewResult(isLast = true) if producer finishes successfully with a final result </li>
 *   <li> onFailure if producer failed to produce a final result </li>
 *   <li> onCancellation if producer was cancelled before a final result could be created </li>
 * </ul>
 *
 * <p> Implementations of this interface must be thread safe, as callback methods might be called
 * on different threads.
 *
 * @param <T>
 */
public interface Consumer<T> {

  /** Status flag used by producers and consumers to supply additional information. */
  @Retention(SOURCE)
  @IntDef(
    flag = true,
    value = {
      IS_LAST,
      DO_NOT_CACHE_ENCODED,
      IS_PLACEHOLDER,
      IS_PARTIAL_RESULT,
      IS_RESIZING_DONE,
    }
  )
  @interface Status {}

  /**
   * Convenience constant for a status with no flags set. The absence of {@link #IS_LAST} means this
   * status can be used for intermediate results. This constant should never be used when checking
   * for flags.
   */
  int NO_FLAGS = 0;
  /**
   * Status flag to show whether the result being received is the last one coming or to expect more.
   */
  int IS_LAST = 1;
  /**
   * Status flag to show the result should not be cached in disk or encoded caches, even if it's the
   * last result.
   */
  int DO_NOT_CACHE_ENCODED = 1 << 1;
  /**
   * Status flag to show whether the result is a placeholder for the final result. Should only be
   * set if IS_LAST is not set.
   */
  int IS_PLACEHOLDER = 1 << 2;
  /**
   * Status flag to show the result does not represent the whole image, just part of it. This may be
   * due to a cancellation or failure while the file was being downloaded or because only part of
   * the image was requested.
   */
  int IS_PARTIAL_RESULT = 1 << 3;
  /** Status flag that indicates whether the given image has been resized. */
  int IS_RESIZING_DONE = 1 << 4;

  /**
   * Called by a producer whenever new data is produced. This method should not throw an exception.
   *
   * <p> In case when result is closeable resource producer will close it after onNewResult returns.
   * Consumer needs to make copy of it if the resource must be accessed after that. Fortunately,
   * with CloseableReferences, that should not impose too much overhead.
   *
   * @param newResult
   * @param status bitwise values describing the returned result
   * @see Status for status flags
   */
  void onNewResult(T newResult, @Status int status);

  /**
   * Called by a producer whenever it terminates further work due to Throwable being thrown. This
   * method should not throw an exception.
   *
   * @param t
   */
  void onFailure(Throwable t);

  /**
   * Called by a producer whenever it is cancelled and won't produce any more results
   */
  void onCancellation();

  /**
   * Called when the progress updates.
   *
   * @param progress in range [0, 1]
   */
  void onProgressUpdate(float progress);
}
