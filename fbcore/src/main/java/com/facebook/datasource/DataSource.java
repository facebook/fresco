/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.datasource;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * An alternative to Java Futures for the image pipeline.
 *
 * <p>Unlike Futures, DataSource can issue a series of results, rather than just one. A prime
 * example is decoding progressive images, which have a series of intermediate results before the
 * final one.
 *
 * <p>DataSources MUST be closed (close() is called on the DataSource) else resources may leak.
 *
 *@param <T> the type of the result
 */
public interface DataSource<T> {

  /**
   * @return true if the data source is closed, false otherwise
   */
  boolean isClosed();

  /**
   * The most recent result of the asynchronous computation.
   *
   * <p>The caller gains ownership of the object and is responsible for releasing it.
   * Note that subsequent calls to getResult might give different results. Later results should be
   * considered to be of higher quality.
   *
   * <p>This method will return null in the following cases:
   * <ul>
   * <li>when the DataSource does not have a result ({@code hasResult} returns false).
   * <li>when the last result produced was null.
   * </ul>
   * @return current best result
   */
  @Nullable T getResult();

  /**
   * @return true if any result (possibly of lower quality) is available right now, false otherwise
   */
  boolean hasResult();

  /**
   * @return true if the data source has multiple results (e.g. multiple images). This can be used
   *     for example by a RetainingDataSource to correctly display images.
   */
  boolean hasMultipleResults();

  /**
   * @return true if request is finished, false otherwise
   */
  boolean isFinished();

  /**
   * @return true if request finished due to error
   */
  boolean hasFailed();

  /**
   * @return failure cause if the source has failed, else null
   */
  @Nullable Throwable getFailureCause();

  /**
   * @return progress in range [0, 1]
   */
  float getProgress();

  /**
   * Cancels the ongoing request and releases all associated resources.
   *
   * <p>Subsequent calls to {@link #getResult} will return null.
   * @return true if the data source is closed for the first time
   */
  boolean close();

  /**
   * Subscribe for notifications whenever the state of the DataSource changes.
   *
   * <p>All changes will be observed on the provided executor.
   * @param dataSubscriber
   * @param executor
   */
  void subscribe(DataSubscriber<T> dataSubscriber, Executor executor);
}
