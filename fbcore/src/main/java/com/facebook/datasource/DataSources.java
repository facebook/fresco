/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.datasource;

import com.facebook.common.internal.Supplier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to the {@link DataSource} interface.
 */
public class DataSources {

  private DataSources() {
  }

  public static <T> DataSource<T> immediateFailedDataSource(Throwable failure) {
    SimpleDataSource<T> simpleDataSource = SimpleDataSource.create();
    simpleDataSource.setFailure(failure);
    return simpleDataSource;
  }

  public static <T> DataSource<T> immediateDataSource(T result) {
    SimpleDataSource<T> simpleDataSource = SimpleDataSource.create();
    simpleDataSource.setResult(result);
    return simpleDataSource;
  }

  public static <T> Supplier<DataSource<T>> getFailedDataSourceSupplier(final Throwable failure) {
    return new Supplier<DataSource<T>>() {
      @Override
      public DataSource<T> get() {
        return DataSources.immediateFailedDataSource(failure);
      }
    };
  }

  /**
   * This methods blocks the calling thread until the {@link DataSource} has a final result, has
   * been cancelled or has failed.
   *
   * @param dataSource The {@link DataSource} to wait for. The caller MUST close the data source
   * after this method returned!
   * @param <T> The type parameter for the {@link DataSource}
   *
   * @return The final result of the {@link DataSource}. Intermediate results are ignored. Might be
   * <code>null</code> if the data source has been cancelled.
   *
   * @throws Throwable if the {@link DataSource} has failed
   */
  @Nullable
  public static <T> T waitForFinalResult(DataSource<T> dataSource) throws Throwable {
    final CountDownLatch latch = new CountDownLatch(1);
    final ValueHolder<T> resultHolder = new ValueHolder<>();
    final ValueHolder<Throwable> pendingException = new ValueHolder<>();

    dataSource.subscribe(
        new DataSubscriber<T>() {
          @Override
          public void onNewResult(DataSource<T> dataSource) {
            // only return the final result
            if (!dataSource.isFinished()) {
              return;
            }

            try {
              resultHolder.value = dataSource.getResult();
            } finally {
              latch.countDown();
            }
          }

          @Override
          public void onFailure(DataSource<T> dataSource) {
            try {
              pendingException.value = dataSource.getFailureCause();
            } finally {
              latch.countDown();
            }
          }

          @Override
          public void onCancellation(DataSource<T> dataSource) {
            // will make the outer method return null
            latch.countDown();
          }

          @Override
          public void onProgressUpdate(DataSource<T> dataSource) {
            // intentionally left blank
          }
        },
        new Executor() {
          @Override
          public void execute(Runnable command) {
            // it's fair to run these short methods on the datasource thread to avoid
            // context switching
            command.run();
          }
        });

    // wait for countdown() by the DataSubscriber
    latch.await();

    // if the data source failed, throw its exception
    if (pendingException.value != null) {
      throw pendingException.value;
    }

    return resultHolder.value;
  }

  private static class ValueHolder<T> {

    @Nullable
    public T value = null;
  }
}
