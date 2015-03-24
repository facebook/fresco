/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.datasource;

import javax.annotation.concurrent.ThreadSafe;

import com.facebook.datasource.DataSource;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.listener.RequestListener;

/**
 * DataSource<T> backed by a Producer<T>
 *
 * @param <T>
 */
@ThreadSafe
public class ProducerToDataSourceAdapter<T>
    extends AbstractProducerToDataSourceAdapter<T> {

  public static <T> DataSource<T> create(
      Producer<T> producer,
      SettableProducerContext settableProducerContext,
      RequestListener listener) {
    return new ProducerToDataSourceAdapter<T>(
        producer,
        settableProducerContext,
        listener);
  }

  private ProducerToDataSourceAdapter(
      Producer<T> producer,
      SettableProducerContext settableProducerContext,
      RequestListener listener) {
    super(producer, settableProducerContext, listener);
  }
}
