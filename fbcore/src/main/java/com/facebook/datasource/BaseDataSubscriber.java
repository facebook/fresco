/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.datasource;

/**
 * Base implementation of {@link DataSubscriber} that ensures that the data source is closed when
 * the subscriber has finished with it.
 * <p>
 * Sample usage:
 * <pre>
 * <code>
 * dataSource.subscribe(
 *   new BaseDataSubscriber() {
 *     {@literal @}Override
 *     public void onNewResultImpl(DataSource dataSource) {
 *       // Store image ref to be released later.
 *       mCloseableImageRef = dataSource.getResult();
 *       // Use the image.
 *       updateImage(mCloseableImageRef);
 *       // No need to do any cleanup of the data source.
 *     }
 *
 *     {@literal @}Override
 *     public void onFailureImpl(DataSource dataSource) {
 *       // No cleanup of the data source required here.
 *     }
 *   });
 * </code>
 * </pre>
 */
public abstract class BaseDataSubscriber<T> implements DataSubscriber<T> {

  @Override
  public void onNewResult(DataSource<T> dataSource) {
    // isFinished() should be checked before calling onNewResultImpl(), otherwise
    // there would be a race condition: the final data source result might be ready before
    // we call isFinished() here, which would lead to the loss of the final result
    // (because of an early dataSource.close() call).
    final boolean shouldClose = dataSource.isFinished();
    try {
      onNewResultImpl(dataSource);
    } finally {
      if (shouldClose) {
        dataSource.close();
      }
    }
  }

  @Override
  public void onFailure(DataSource<T> dataSource) {
    try {
      onFailureImpl(dataSource);
    } finally {
      dataSource.close();
    }
  }

  @Override
  public void onCancellation(DataSource<T> dataSource) {
  }

  @Override
  public void onProgressUpdate(DataSource<T> dataSource) {
  }

  protected abstract void onNewResultImpl(DataSource<T> dataSource);

  protected abstract void onFailureImpl(DataSource<T> dataSource);
}
