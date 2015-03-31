/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.datasource;

import com.facebook.common.internal.Preconditions;

/**
 * Settable {@link DataSource}.
 */
public class SettableDataSource<T> extends AbstractDataSource<T> {

  private SettableDataSource() {
  }

  /**
   * Creates a new {@link SettableDataSource}.
   */
  public static <T> SettableDataSource<T> create() {
    return new SettableDataSource<T>();
  }

  /**
   * Sets the result to {@code value}.
   *
   * <p> This method will return {@code true} if the value was successfully set, or
   * {@code false} if the data source has already been set, failed or closed.
   *
   * <p> If the value was successfully set and {@code isLast} is {@code true}, state of the
   * data source will be set to {@link AbstractDataSource.DataSourceStatus#SUCCESS}.
   *
   * <p> This will also notify the subscribers if the value was successfully set.
   *
   * @param value the value to be set
   * @param isLast whether or not the value is last.
   * @return true if the value was successfully set.
   */
  @Override
  public boolean setResult(T value, boolean isLast) {
    return super.setResult(Preconditions.checkNotNull(value), isLast);
  }

  /**
   * Sets the value as the last result.
   * <p> See {@link #setResult(T value, boolean isLast)}.
   */
  public boolean setResult(T value) {
    return super.setResult(Preconditions.checkNotNull(value), /* isLast */ true);
  }

  /**
   * Sets the failure.
   *
   * <p> This method will return {@code true} if the failure was successfully set, or
   * {@code false} if the data source has already been set, failed or closed.
   *
   * <p> If the failure was successfully set, state of the data source will be set to
   * {@link AbstractDataSource.DataSourceStatus#FAILURE}.
   *
   * <p> This will also notify the subscribers if the failure was successfully set.
   *
   * @param throwable the failure cause to be set.
   * @return true if the failure was successfully set.
   */
  @Override
  public boolean setFailure(Throwable throwable) {
    return super.setFailure(Preconditions.checkNotNull(throwable));
  }

  /**
   * Sets the progress.
   *
   * @param progress the progress in range [0, 1] to be set.
   * @return true if the progress was successfully set.
   */
  @Override
  public boolean setProgress(float progress) {
    return super.setProgress(progress);
  }
}
