/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource;

import com.facebook.common.internal.Preconditions;
import com.facebook.datasource.AbstractDataSource;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.BaseConsumer;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.request.HasImageRequest;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
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
  private final RequestListener mRequestListener;

  protected AbstractProducerToDataSourceAdapter(
      Producer<T> producer,
      SettableProducerContext settableProducerContext,
      RequestListener requestListener) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractProducerToDataSourceAdapter()");
    }
    mSettableProducerContext = settableProducerContext;
    mRequestListener = requestListener;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractProducerToDataSourceAdapter()->onRequestStart");
    }
    mRequestListener.onRequestStart(
        settableProducerContext.getImageRequest(),
        mSettableProducerContext.getCallerContext(),
        mSettableProducerContext.getId(),
        mSettableProducerContext.isPrefetch());
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
        AbstractProducerToDataSourceAdapter.this.onNewResultImpl(newResult, status);
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

  protected void onNewResultImpl(@Nullable T result, int status) {
    boolean isLast = BaseConsumer.isLast(status);
    if (super.setResult(result, isLast)) {
      if (isLast) {
        mRequestListener.onRequestSuccess(
            mSettableProducerContext.getImageRequest(),
            mSettableProducerContext.getId(),
            mSettableProducerContext.isPrefetch());
      }
    }
  }

  private void onFailureImpl(Throwable throwable) {
    if (super.setFailure(throwable)) {
      mRequestListener.onRequestFailure(
          mSettableProducerContext.getImageRequest(),
          mSettableProducerContext.getId(),
          throwable,
          mSettableProducerContext.isPrefetch());
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
      mRequestListener.onRequestCancellation(mSettableProducerContext.getId());
      mSettableProducerContext.cancel();
    }
    return true;
  }
}
