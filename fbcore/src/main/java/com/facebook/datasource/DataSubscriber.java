/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.datasource;

import javax.annotation.Nonnull;

/**
 * Subscribes to DataSource<T>.
 *
 * @param <T>
 */
public interface DataSubscriber<T> {

  /**
   * Called whenever a new value is ready to be retrieved from the DataSource.
   *
   * <p>To retrieve the new value, call {@code dataSource.getResult()}.
   *
   * <p>To determine if the new value is the last, use {@code dataSource.isFinished()}.
   *
   * @param dataSource
   */
  void onNewResult(@Nonnull DataSource<T> dataSource);

  /**
   * Called whenever an error occurs inside of the pipeline.
   *
   * <p>No further results will be produced after this method is called.
   *
   * <p>The throwable resulting from the failure can be obtained using {@code
   * dataSource.getFailureCause}.
   *
   * @param dataSource
   */
  void onFailure(@Nonnull DataSource<T> dataSource);

  /**
   * Called whenever the request is cancelled (a request being cancelled means that is was closed
   * before it finished).
   *
   * <p>No further results will be produced after this method is called.
   *
   * @param dataSource
   */
  void onCancellation(@Nonnull DataSource<T> dataSource);

  /**
   * Called when the progress updates.
   *
   * @param dataSource
   */
  void onProgressUpdate(@Nonnull DataSource<T> dataSource);
}
