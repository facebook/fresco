/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource;

import com.facebook.common.internal.Preconditions;
import com.facebook.datasource.AbstractDataSource;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.producers.BaseConsumer;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.request.HasImageRequest;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * DataSource<T> backed by a Producer<T>
 *
 * @param <T>
 */
@ThreadSafe
public abstract class AbstractProducerToDataSourceAdapter<T> extends AbstractDataSource<T>
    implements HasImageRequest {

  private final SettableProducerContext mSettableProducerContext;
  private final RequestListener2 mRequestListener;

  protected AbstractProducerToDataSourceAdapter(
      Producer<T> producer,
      SettableProducerContext settableProducerContext,
      RequestListener2 requestListener) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractProducerToDataSourceAdapter()");
    }
    mSettableProducerContext = settableProducerContext;
    mRequestListener = requestListener;
    setInitialExtras();
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractProducerToDataSourceAdapter()->onRequestStart");
    }
    mRequestListener.onRequestStart(mSettableProducerContext);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractProducerToDataSourceAdapter()->produceResult");
    }
    producer.produceResults(createConsumer(), settableProducerContext);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  private Consumer<T> createConsumer() {
    return new BaseConsumer<T>() {
      @Override
      protected void onNewResultImpl(@Nullable T newResult, @Status int status) {
        AbstractProducerToDataSourceAdapter.this.onNewResultImpl(
            newResult, status, mSettableProducerContext);
      }

      @Override
      protected void onFailureImpl(Throwable throwable) {
        AbstractProducerToDataSourceAdapter.this.onFailureImpl(throwable);
      }

      @Override
      protected void onCancellationImpl() {
        AbstractProducerToDataSourceAdapter.this.onCancellationImpl();
      }

      @Override
      protected void onProgressUpdateImpl(float progress) {
        AbstractProducerToDataSourceAdapter.this.setProgress(progress);
      }
    };
  }

  protected void onNewResultImpl(@Nullable T result, int status, ProducerContext producerContext) {
    boolean isLast = BaseConsumer.isLast(status);
    if (super.setResult(result, isLast, getExtras(producerContext))) {
      if (isLast) {
        mRequestListener.onRequestSuccess(mSettableProducerContext);
      }
    }
  }

  protected Map<String, Object> getExtras(ProducerContext producerContext) {
    return producerContext.getExtras();
  }

  private void onFailureImpl(Throwable throwable) {
    if (super.setFailure(throwable, getExtras(mSettableProducerContext))) {
      mRequestListener.onRequestFailure(mSettableProducerContext, throwable);
    }
  }

  private synchronized void onCancellationImpl() {
    Preconditions.checkState(isClosed());
  }

  @Override
  public ImageRequest getImageRequest() {
    return mSettableProducerContext.getImageRequest();
  }

  @Override
  public boolean close() {
    if (!super.close()) {
      return false;
    }
    if (!super.isFinished()) {
      mRequestListener.onRequestCancellation(mSettableProducerContext);
      mSettableProducerContext.cancel();
    }
    return true;
  }

  private void setInitialExtras() {
    setExtras(mSettableProducerContext.getExtras());
  }
}
