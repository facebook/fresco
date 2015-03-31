/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.datasource;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.facebook.datasource.AbstractDataSource;
import com.facebook.datasource.DataSource;
import com.facebook.common.references.CloseableReference;

/**
 * A {@link DataSource} whose result may be set by a {@link #set(CloseableReference<T>)} or
 * {@link #setException(Throwable)} call. It may also be closed.
 *
 * <p>This data source has no intermediate results - calling {@link #set(CloseableReference<T>)}
 * means that the data source is finished.
 */
@ThreadSafe
public final class SettableDataSource<T> extends AbstractDataSource<CloseableReference<T>> {

  /**
   * Creates a new {@code SettableDataSource}
   */
  public static <V> SettableDataSource<V> create() {
    return new SettableDataSource<V>();
  }

  private SettableDataSource() {
  }

  /**
   * Sets the value of this data source.
   *
   * <p> This method will return {@code true} if the value was successfully set, or
   * {@code false} if the data source has already been set, failed or closed.
   *
   * <p> Passed CloseableReference is cloned, caller of this method still owns passed reference
   * after the method returns.
   *
   * @param valueRef closeable reference to the value the data source should hold.
   * @return true if the value was successfully set.
   */
  public boolean set(@Nullable CloseableReference<T> valueRef) {
    CloseableReference<T> clonedRef = CloseableReference.cloneOrNull(valueRef);
    return super.setResult(clonedRef, /* isLast */ true);
  }

  /**
   * Sets the data source to having failed with the given exception.
   *
   * <p> This method will return {@code true} if the exception was successfully set, or
   * {@code false} if the data source has already been set, failed or closed.
   *
   * @param throwable the exception the data source should hold.
   * @return true if the exception was successfully set.
   */
  public boolean setException(Throwable throwable) {
    return super.setFailure(throwable);
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

  /**
   * Gets the result if any, null otherwise.
   *
   * <p> Value will be cloned and it's the caller's responsibility to close the returned value.
   */
  @Override
  @Nullable
  public CloseableReference<T> getResult() {
    return CloseableReference.cloneOrNull(super.getResult());
  }

  @Override
  protected void closeResult(@Nullable CloseableReference<T> result) {
    CloseableReference.closeSafely(result);
  }
}
