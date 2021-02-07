/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestListener2;
import javax.annotation.Nullable;

public class InternalRequestListener extends InternalProducerListener implements RequestListener2 {

  private final @Nullable RequestListener mRequestListener;
  private final @Nullable RequestListener2 mRequestListener2;

  public InternalRequestListener(
      @Nullable RequestListener requestListener, @Nullable RequestListener2 requestListener2) {
    super(requestListener, requestListener2);
    mRequestListener = requestListener;
    mRequestListener2 = requestListener2;
  }

  @Override
  public void onRequestStart(ProducerContext producerContext) {
    if (mRequestListener != null) {
      mRequestListener.onRequestStart(
          producerContext.getImageRequest(),
          producerContext.getCallerContext(),
          producerContext.getId(),
          producerContext.isPrefetch());
    }
    if (mRequestListener2 != null) {
      mRequestListener2.onRequestStart(producerContext);
    }
  }

  @Override
  public void onRequestSuccess(ProducerContext producerContext) {
    if (mRequestListener != null) {
      mRequestListener.onRequestSuccess(
          producerContext.getImageRequest(), producerContext.getId(), producerContext.isPrefetch());
    }
    if (mRequestListener2 != null) {
      mRequestListener2.onRequestSuccess(producerContext);
    }
  }

  @Override
  public void onRequestFailure(ProducerContext producerContext, Throwable throwable) {
    if (mRequestListener != null) {
      mRequestListener.onRequestFailure(
          producerContext.getImageRequest(),
          producerContext.getId(),
          throwable,
          producerContext.isPrefetch());
    }
    if (mRequestListener2 != null) {
      mRequestListener2.onRequestFailure(producerContext, throwable);
    }
  }

  @Override
  public void onRequestCancellation(ProducerContext producerContext) {
    if (mRequestListener != null) {
      mRequestListener.onRequestCancellation(producerContext.getId());
    }
    if (mRequestListener2 != null) {
      mRequestListener2.onRequestCancellation(producerContext);
    }
  }
}
