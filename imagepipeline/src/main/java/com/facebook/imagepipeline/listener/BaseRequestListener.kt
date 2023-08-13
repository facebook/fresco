/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener

import com.facebook.imagepipeline.request.ImageRequest

open class BaseRequestListener : RequestListener {

  override fun onRequestStart(
      request: ImageRequest,
      callerContext: Any,
      requestId: String,
      isPrefetch: Boolean
  ) = Unit

  override fun onRequestSuccess(request: ImageRequest, requestId: String, isPrefetch: Boolean) =
      Unit

  override fun onRequestFailure(
      request: ImageRequest,
      requestId: String,
      throwable: Throwable,
      isPrefetch: Boolean
  ) = Unit

  override fun onRequestCancellation(requestId: String) = Unit

  override fun onProducerStart(requestId: String, producerName: String) = Unit

  override fun onProducerEvent(requestId: String, producerName: String, eventName: String) = Unit

  override fun onProducerFinishWithSuccess(
      requestId: String,
      producerName: String,
      extraMap: Map<String, String>?
  ) = Unit

  override fun onProducerFinishWithFailure(
      requestId: String,
      producerName: String,
      t: Throwable,
      extraMap: Map<String, String>?
  ) = Unit

  override fun onProducerFinishWithCancellation(
      requestId: String,
      producerName: String,
      extraMap: Map<String, String>?
  ) = Unit

  override fun onUltimateProducerReached(
      requestId: String,
      producerName: String,
      successful: Boolean
  ) = Unit

  override fun requiresExtraMap(requestId: String): Boolean = false
}
