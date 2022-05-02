/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.datasource;

import com.facebook.infer.annotation.Nullsafe;

/**
 * Base implementation of {@link DataSubscriber} that ensures that the data source is closed when
 * the subscriber has finished with it.
 *
 * <p>Sample usage:
 *
 * <pre>
 * <code>
 * imagePipeline.isInDiskCache(
 * uri,
 * new BaseBooleanSubscriber() {
 *   public void onNewResultImpl(boolean isFound) {
 *     // caller's code here
 *   }
 * });
 * </code>
 * </pre>
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class BaseBooleanSubscriber implements DataSubscriber<Boolean> {
  @Override
  public void onNewResult(DataSource<Boolean> dataSource) {
    try {
      // NULLSAFE_FIXME[Parameter Not Nullable]
      onNewResultImpl(dataSource.getResult());
    } finally {
      dataSource.close();
    }
  }

  @Override
  public void onFailure(DataSource<Boolean> dataSource) {
    try {
      onFailureImpl(dataSource);
    } finally {
      dataSource.close();
    }
  }

  @Override
  public void onCancellation(DataSource<Boolean> dataSource) {}

  @Override
  public void onProgressUpdate(DataSource<Boolean> dataSource) {}

  protected abstract void onNewResultImpl(boolean isFoundInDisk);

  protected abstract void onFailureImpl(DataSource<Boolean> dataSource);
}
