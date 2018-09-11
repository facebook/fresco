/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.concurrent.ThreadSafe;

/**
 * ProducerContext that allows the client to change its internal state.
 */
@ThreadSafe
public class SettableProducerContext extends BaseProducerContext {

  public SettableProducerContext(ProducerContext context) {
    this(
        context.getImageRequest(),
        context.getId(),
        context.getListener(),
        context.getCallerContext(),
        context.getLowestPermittedRequestLevel(),
        context.isPrefetch(),
        context.isIntermediateResultExpected(),
        context.getPriority());
  }

  public SettableProducerContext(ImageRequest overrideRequest, ProducerContext context) {
    this(
        overrideRequest,
        context.getId(),
        context.getListener(),
        context.getCallerContext(),
        context.getLowestPermittedRequestLevel(),
        context.isPrefetch(),
        context.isIntermediateResultExpected(),
        context.getPriority());
  }

  public SettableProducerContext(
      ImageRequest imageRequest,
      String id,
      ProducerListener producerListener,
      Object callerContext,
      ImageRequest.RequestLevel lowestPermittedRequestLevel,
      boolean isPrefetch,
      boolean isIntermediateResultExpected,
      Priority priority) {
    super(
        imageRequest,
        id,
        producerListener,
        callerContext,
        lowestPermittedRequestLevel,
        isPrefetch,
        isIntermediateResultExpected,
        priority);
  }

  /**
   * Set whether the request is a prefetch request or not.
   * @param isPrefetch
   */
  public void setIsPrefetch(boolean isPrefetch) {
    BaseProducerContext.callOnIsPrefetchChanged(setIsPrefetchNoCallbacks(isPrefetch));
  }

  /**
   * Set whether intermediate result is expected or not
   * @param isIntermediateResultExpected
   */
  public void setIsIntermediateResultExpected(boolean isIntermediateResultExpected) {
    BaseProducerContext.callOnIsIntermediateResultExpectedChanged(
        setIsIntermediateResultExpectedNoCallbacks(isIntermediateResultExpected));
  }

  /**
   * Set the priority of the request
   * @param priority
   */
  public void setPriority(Priority priority) {
    BaseProducerContext.callOnPriorityChanged(setPriorityNoCallbacks(priority));
  }

}
