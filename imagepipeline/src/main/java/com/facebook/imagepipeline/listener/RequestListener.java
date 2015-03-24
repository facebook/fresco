/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.listener;

import com.facebook.imagepipeline.producers.ProducerListener;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Listener for {@link ImageRequest}.
 */
public interface RequestListener extends ProducerListener {
  /**
   * Called when request is about to be submitted to the Orchestrator's executor queue.
   * @param request which triggered the event
   * @param callerContext context of the caller of the request
   * @param requestId unique id generated automatically for each request submission
   * @param isPrefetch whether the request is a prefetch or not
   */
  void onRequestStart(
      ImageRequest request,
      Object callerContext,
      String requestId,
      boolean isPrefetch);

  /**
   * Called after successful completion of the request (all producers completed successfully).
   * @param request which triggered the event
   * @param requestId unique id generated automatically for each request submission
   * @param isPrefetch whether the request is a prefetch or not
   */
  void onRequestSuccess(ImageRequest request, String requestId, boolean isPrefetch);

  /**
   * Called after failure to complete the request (some producer failed).
   * @param request which triggered the event
   * @param requestId unique id generated automatically for each request submission
   * @param throwable cause of failure
   * @param isPrefetch whether the request is a prefetch or not
   */
  void onRequestFailure(
      ImageRequest request,
      String requestId,
      Throwable throwable,
      boolean isPrefetch);

  /**
   * Called after the request is cancelled.
   * @param requestId unique id generated automatically for each request submission
   */
  void onRequestCancellation(String requestId);
}
