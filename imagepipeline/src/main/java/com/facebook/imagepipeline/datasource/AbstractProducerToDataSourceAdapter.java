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
import javax.annotation.concurrent.ThreadSafe;

import com.facebook.common.internal.Preconditions;
import com.facebook.datasource.AbstractDataSource;
import com.facebook.imagepipeline.producers.BaseConsumer;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.listener.RequestListener;

/**
 * DataSource<T> backed by a Producer<T>
 *
 * @param <T>
 */
@ThreadSafe
public abstract class AbstractProducerToDataSourceAdapter<T> extends AbstractDataSource<T> {

  private final SettableProducerContext mSettableProducerContext;
  private final RequestListener mRequestListener;

  protected AbstractProducerToDataSourceAdapter(
      Producer<T> producer,
      SettableProducerContext settableProducerContext,
      RequestListener requestListener) {
    mSettableProducerContext = settableProducerContext;
    mRequestListener = requestListener;
    mRequestListener.onRequestStart(
        settableProducerContext.getImageRequest(),
        mSettableProducerContext.getCallerContext(),
        mSettableProducerContext.getId(),
        mSettableProducerContext.isPrefetch());
    producer.produceResults(createConsumer(), settableProducerContext);
  }

  private Consumer<T> createConsumer() {
    return new BaseConsumer<T>() {
      @Override
      protected void onNewResultImpl(@Nullable T newResult, boolean isLast) {
        AbstractProducerToDataSourceAdapter.this.onNewResultImpl(newResult, isLast);
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

  protected void onNewResultImpl(@Nullable T result, boolean isLast) {
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
