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
import javax.annotation.concurrent.GuardedBy;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.AbstractDataSource;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;

/**
 * Data source that wraps number of other data sources and waits until all of them are finished.
 * After that each call to getResult() returns list of final results of wrapped data sources.
 * Caller of getResult() is responsible for closing all each of the results separately.
 *
 * <p> This data source does not propagate intermediate results.
 *
 * @param <T>
 */
public class ListDataSource<T> extends AbstractDataSource<ArrayList<CloseableReference<T>>> {
  private final DataSource<CloseableReference<T>>[] mDataSources;
  @GuardedBy("this")
  private int mFinishedDataSources;

  protected ListDataSource(DataSource<CloseableReference<T>>[] dataSources) {
    mDataSources = dataSources;
    mFinishedDataSources = 0;
  }

  public static <T> ListDataSource<T> create(
      DataSource<CloseableReference<T>>... dataSources) {
    Preconditions.checkNotNull(dataSources);
    Preconditions.checkState(dataSources.length > 0);
    ListDataSource<T> listDataSource = new ListDataSource<T>(dataSources);
    for (DataSource<CloseableReference<T>> dataSource : dataSources) {
      dataSource.subscribe(
          listDataSource.new InternalDataSubscriber(),
          CallerThreadExecutor.getInstance());
    }
    return listDataSource;
  }

  @Override
  @Nullable
  public synchronized ArrayList<CloseableReference<T>> getResult() {
    if (!hasResult()) {
      return null;
    }
    ArrayList<CloseableReference<T>> results = new ArrayList<>(mDataSources.length);
    for (DataSource<CloseableReference<T>> dataSource : mDataSources) {
      results.add(dataSource.getResult());
    }
    return results;
  }

  @Override
  public synchronized boolean hasResult() {
    return !isClosed() && (mFinishedDataSources == mDataSources.length);
  }

  @Override
  public boolean close() {
    if (!super.close()) {
      return false;
    }
    for (DataSource<?> dataSource : mDataSources) {
      dataSource.close();
    }
    return true;
  }

  private void onDataSourceFinished() {
    if (increaseAndCheckIfLast()) {
      setResult(null, /* isLast */ true);
    }
  }

  private synchronized boolean increaseAndCheckIfLast() {
    return ++mFinishedDataSources == mDataSources.length;
  }

  private void onDataSourceFailed(DataSource<CloseableReference<T>> dataSource) {
    setFailure(dataSource.getFailureCause());
  }

  private void onDataSourceCancelled() {
    setFailure(new CancellationException());
  }

  private void onDataSourceProgress() {
    float progress = 0;
    for (DataSource<?> dataSource : mDataSources) {
      progress += dataSource.getProgress();
    }
    setProgress(progress / mDataSources.length);
  }

  private class InternalDataSubscriber implements DataSubscriber<CloseableReference<T>> {
    @GuardedBy("InternalDataSubscriber.this")
    boolean mFinished = false;

    private synchronized boolean tryFinish() {
      if (mFinished) {
        return false;
      }
      mFinished = true;
      return true;
    }

    @Override
    public void onFailure(DataSource<CloseableReference<T>> dataSource) {
      ListDataSource.this.onDataSourceFailed(dataSource);
    }

    @Override
    public void onCancellation(DataSource<CloseableReference<T>> dataSource) {
      ListDataSource.this.onDataSourceCancelled();
    }

    @Override
    public void onNewResult(DataSource<CloseableReference<T>> dataSource) {
      if (dataSource.isFinished() && tryFinish()) {
        ListDataSource.this.onDataSourceFinished();
      }
    }

    @Override
    public void onProgressUpdate(DataSource<CloseableReference<T>> dataSource) {
      ListDataSource.this.onDataSourceProgress();
    }
  }
}
