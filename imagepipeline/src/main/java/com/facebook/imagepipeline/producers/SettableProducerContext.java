/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.concurrent.ThreadSafe;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * ProducerContext that allows the client to change its internal state.
 */
@ThreadSafe
public class SettableProducerContext extends BaseProducerContext {

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
