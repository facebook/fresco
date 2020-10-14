/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * DataSource<CloseableReference<T>> backed by a Producer<CloseableReference<T>>
 *
 * @param <T>
 */
@Nullsafe(Nullsafe.Mode.STRICT)
@ThreadSafe
public class CloseableProducerToDataSourceAdapter<T>
    extends AbstractProducerToDataSourceAdapter<CloseableReference<T>> {

  public static <T> DataSource<CloseableReference<T>> create(
      Producer<CloseableReference<T>> producer,
      SettableProducerContext settableProducerContext,
      RequestListener2 listener) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("CloseableProducerToDataSourceAdapter#create");
    }
    CloseableProducerToDataSourceAdapter<T> result =
        new CloseableProducerToDataSourceAdapter<T>(producer, settableProducerContext, listener);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return result;
  }

  private CloseableProducerToDataSourceAdapter(
      Producer<CloseableReference<T>> producer,
      SettableProducerContext settableProducerContext,
      RequestListener2 listener) {
    super(producer, settableProducerContext, listener);
  }

  @Override
  @Nullable
  public CloseableReference<T> getResult() {
    return CloseableReference.cloneOrNull(super.getResult());
  }

  @Override
  protected void closeResult(CloseableReference<T> result) {
    CloseableReference.closeSafely(result);
  }

  @Override
  protected void onNewResultImpl(
      CloseableReference<T> result, int status, ProducerContext producerContext) {
    super.onNewResultImpl(CloseableReference.cloneOrNull(result), status, producerContext);
  }
}
