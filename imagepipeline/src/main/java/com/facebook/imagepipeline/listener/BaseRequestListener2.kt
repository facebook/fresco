/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener

import com.facebook.imagepipeline.producers.ProducerContext

open class BaseRequestListener2 : RequestListener2 {

  override fun onRequestStart(producerContext: ProducerContext) = Unit

  override fun onRequestSuccess(producerContext: ProducerContext) = Unit

  override fun onRequestFailure(producerContext: ProducerContext, throwable: Throwable?) = Unit

  override fun onRequestCancellation(producerContext: ProducerContext) = Unit

  override fun onProducerStart(producerContext: ProducerContext, producerName: String) = Unit

  override fun onProducerEvent(
      producerContext: ProducerContext,
      producerName: String,
      eventName: String
  ) = Unit

  override fun onProducerFinishWithSuccess(
      producerContext: ProducerContext,
      producerName: String,
      extraMap: Map<String, String>?
  ) = Unit

  override fun onProducerFinishWithFailure(
      producerContext: ProducerContext,
      producerName: String,
      t: Throwable?,
      extraMap: Map<String, String>?
  ) = Unit

  override fun onProducerFinishWithCancellation(
      producerContext: ProducerContext,
      producerName: String,
      extraMap: Map<String, String>?
  ) = Unit

  override fun onUltimateProducerReached(
      producerContext: ProducerContext,
      producerName: String,
      successful: Boolean
  ) = Unit

  override fun requiresExtraMap(producerContext: ProducerContext, producerName: String): Boolean =
      false
}
