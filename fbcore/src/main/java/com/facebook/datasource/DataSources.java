/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.datasource;


import com.facebook.common.internal.Supplier;

/**
 * Static utility methods pertaining to the {@link DataSource} interface.
 */
public class DataSources {

  private DataSources() {
  }

  public static <T> DataSource<T> immediateFailedDataSource(Throwable failure) {
    SettableDataSource<T> settableDataSource = SettableDataSource.create();
    settableDataSource.setFailure(failure);
    return settableDataSource;
  }

  public static <T> Supplier<DataSource<T>> getFailedDataSourceSupplier(final Throwable failure) {
    return new Supplier<DataSource<T>>() {
      @Override
      public DataSource<T> get() {
        return DataSources.immediateFailedDataSource(failure);
      }
    };
  }
}
