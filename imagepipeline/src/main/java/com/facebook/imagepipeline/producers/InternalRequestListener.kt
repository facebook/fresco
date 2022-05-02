/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.listener.RequestListener2

class InternalRequestListener(
    private val requestListener: RequestListener?,
    private val requestListener2: RequestListener2?
) : InternalProducerListener(requestListener, requestListener2), RequestListener2 {

  override fun onRequestStart(producerContext: ProducerContext) {
    requestListener?.onRequestStart(
        producerContext.imageRequest,
        producerContext.callerContext,
        producerContext.id,
        producerContext.isPrefetch)
    requestListener2?.onRequestStart(producerContext)
  }

  override fun onRequestSuccess(producerContext: ProducerContext) {
    requestListener?.onRequestSuccess(
        producerContext.imageRequest, producerContext.id, producerContext.isPrefetch)
    requestListener2?.onRequestSuccess(producerContext)
  }

  override fun onRequestFailure(producerContext: ProducerContext, throwable: Throwable?) {
    requestListener?.onRequestFailure(
        producerContext.imageRequest, producerContext.id, throwable, producerContext.isPrefetch)
    requestListener2?.onRequestFailure(producerContext, throwable)
  }

  override fun onRequestCancellation(producerContext: ProducerContext) {
    requestListener?.onRequestCancellation(producerContext.id)
    requestListener2?.onRequestCancellation(producerContext)
  }
}
