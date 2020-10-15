/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/** ProducerContext that allows the client to change its internal state. */
@Nullsafe(Nullsafe.Mode.STRICT)
@ThreadSafe
public class SettableProducerContext extends BaseProducerContext {

  public SettableProducerContext(ProducerContext context) {
    this(
        context.getImageRequest(),
        context.getId(),
        context.getUiComponentId(),
        context.getProducerListener(),
        context.getCallerContext(),
        context.getLowestPermittedRequestLevel(),
        context.isPrefetch(),
        context.isIntermediateResultExpected(),
        context.getPriority(),
        context.getImagePipelineConfig());
  }

  public SettableProducerContext(ImageRequest overrideRequest, ProducerContext context) {
    this(
        overrideRequest,
        context.getId(),
        context.getUiComponentId(),
        context.getProducerListener(),
        context.getCallerContext(),
        context.getLowestPermittedRequestLevel(),
        context.isPrefetch(),
        context.isIntermediateResultExpected(),
        context.getPriority(),
        context.getImagePipelineConfig());
  }

  public SettableProducerContext(
      ImageRequest imageRequest,
      String id,
      ProducerListener2 producerListener,
      Object callerContext,
      ImageRequest.RequestLevel lowestPermittedRequestLevel,
      boolean isPrefetch,
      boolean isIntermediateResultExpected,
      Priority priority,
      ImagePipelineConfig imagePipelineConfig) {
    super(
        imageRequest,
        id,
        producerListener,
        callerContext,
        lowestPermittedRequestLevel,
        isPrefetch,
        isIntermediateResultExpected,
        priority,
        imagePipelineConfig);
  }

  public SettableProducerContext(
      ImageRequest imageRequest,
      String id,
      @Nullable String uiComponentId,
      ProducerListener2 producerListener,
      Object callerContext,
      ImageRequest.RequestLevel lowestPermittedRequestLevel,
      boolean isPrefetch,
      boolean isIntermediateResultExpected,
      Priority priority,
      ImagePipelineConfig imagePipelineConfig) {
    super(
        imageRequest,
        id,
        uiComponentId,
        producerListener,
        callerContext,
        lowestPermittedRequestLevel,
        isPrefetch,
        isIntermediateResultExpected,
        priority,
        imagePipelineConfig);
  }

  /**
   * Set whether the request is a prefetch request or not.
   *
   * @param isPrefetch
   */
  public void setIsPrefetch(boolean isPrefetch) {
    BaseProducerContext.callOnIsPrefetchChanged(setIsPrefetchNoCallbacks(isPrefetch));
  }

  /**
   * Set whether intermediate result is expected or not
   *
   * @param isIntermediateResultExpected
   */
  public void setIsIntermediateResultExpected(boolean isIntermediateResultExpected) {
    BaseProducerContext.callOnIsIntermediateResultExpectedChanged(
        setIsIntermediateResultExpectedNoCallbacks(isIntermediateResultExpected));
  }

  /**
   * Set the priority of the request
   *
   * @param priority
   */
  public void setPriority(Priority priority) {
    BaseProducerContext.callOnPriorityChanged(setPriorityNoCallbacks(priority));
  }
}
