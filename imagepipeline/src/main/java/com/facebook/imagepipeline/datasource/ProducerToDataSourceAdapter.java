/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource;

import com.facebook.datasource.DataSource;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import javax.annotation.concurrent.ThreadSafe;

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
