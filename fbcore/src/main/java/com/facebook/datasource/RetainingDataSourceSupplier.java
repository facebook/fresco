/*
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.datasource;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Supplier;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class RetainingDataSourceSupplier<T> implements Supplier<DataSource<T>> {

  private final Set<RetainingDataSource> mDataSources =
      Collections.newSetFromMap(new WeakHashMap<RetainingDataSource, Boolean>());

  private @Nullable Supplier<DataSource<T>> mCurrentDataSourceSupplier = null;

  @Override
  public DataSource<T> get() {
    RetainingDataSource dataSource = new RetainingDataSource();
    dataSource.setSupplier(mCurrentDataSourceSupplier);
    mDataSources.add(dataSource);
    return dataSource;
  }

  public void replaceSupplier(Supplier<DataSource<T>> supplier) {
    mCurrentDataSourceSupplier = supplier;
    for (RetainingDataSource dataSource : mDataSources) {
      if (!dataSource.isClosed()) {
        dataSource.setSupplier(supplier);
      }
    }
  }

  private static class RetainingDataSource<T> extends AbstractDataSource<T> {
    @GuardedBy("RetainingDataSource.this")
    @Nullable
    private DataSource<T> mDataSource = null;

    public void setSupplier(@Nullable Supplier<DataSource<T>> supplier) {
      // early return without calling {@code supplier.get()} in case we are closed
      if (isClosed()) {
        return;
      }
      DataSource<T> oldDataSource;
      DataSource<T> newDataSource = (supplier != null) ? supplier.get() : null;
      synchronized (RetainingDataSource.this) {
        if (isClosed()) {
          closeSafely(newDataSource);
          return;
        } else {
          oldDataSource = mDataSource;
          mDataSource = newDataSource;
        }
      }
      if (newDataSource != null) {
        newDataSource.subscribe(new InternalDataSubscriber(), CallerThreadExecutor.getInstance());
      }
      closeSafely(oldDataSource);
    }

    @Override
    @Nullable
    public synchronized T getResult() {
      return (mDataSource != null) ? mDataSource.getResult() : null;
    }

    @Override
    public synchronized boolean hasResult() {
      return (mDataSource != null) && mDataSource.hasResult();
    }

    @Override
    public boolean close() {
      DataSource<T> dataSource;
      synchronized (RetainingDataSource.this) {
        // it's fine to call {@code super.close()} within a synchronized block because we don't
        // implement {@link #closeResult()}, but perform result closing ourselves.
        if (!super.close()) {
          return false;
        }
        dataSource = mDataSource;
        mDataSource = null;
      }
      closeSafely(dataSource);
      return true;
    }

    private void onDataSourceNewResult(DataSource<T> dataSource) {
      if (dataSource == mDataSource) {
        setResult(null, false);
      }
    }

    private void onDataSourceFailed(DataSource<T> dataSource) {
      // do not propagate failure
    }

    private void onDatasourceProgress(DataSource<T> dataSource) {
      if (dataSource == mDataSource) {
        setProgress(dataSource.getProgress());
      }
    }

    private static <T> void closeSafely(DataSource<T> dataSource) {
      if (dataSource != null) {
        dataSource.close();
      }
    }

    private class InternalDataSubscriber implements DataSubscriber<T> {
      @Override
      public void onNewResult(DataSource<T> dataSource) {
        if (dataSource.hasResult()) {
          RetainingDataSource.this.onDataSourceNewResult(dataSource);
        } else if (dataSource.isFinished()) {
          RetainingDataSource.this.onDataSourceFailed(dataSource);
        }
      }

      @Override
      public void onFailure(DataSource<T> dataSource) {
        RetainingDataSource.this.onDataSourceFailed(dataSource);
      }

      @Override
      public void onCancellation(DataSource<T> dataSource) {}

      @Override
      public void onProgressUpdate(DataSource<T> dataSource) {
        RetainingDataSource.this.onDatasourceProgress(dataSource);
      }
    }

    @Override
    public boolean hasMultipleResults() {
      return true;
    }
  }
}
