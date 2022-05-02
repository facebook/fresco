/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

open class InternalProducerListener(
    val producerListener: ProducerListener?,
    val producerListener2: ProducerListener2?
) : ProducerListener2 {

  override fun onProducerStart(context: ProducerContext, producerName: String?) {
    producerListener?.onProducerStart(context.id, producerName)
    producerListener2?.onProducerStart(context, producerName)
  }

  override fun onProducerEvent(
      context: ProducerContext,
      producerName: String?,
      eventName: String?
  ) {
    producerListener?.onProducerEvent(context.id, producerName, eventName)
    producerListener2?.onProducerEvent(context, producerName, eventName)
  }

  override fun onProducerFinishWithSuccess(
      context: ProducerContext,
      producerName: String?,
      extraMap: Map<String?, String?>?
  ) {
    producerListener?.onProducerFinishWithSuccess(context.id, producerName, extraMap)
    producerListener2?.onProducerFinishWithSuccess(context, producerName, extraMap)
  }

  override fun onProducerFinishWithFailure(
      context: ProducerContext,
      producerName: String?,
      t: Throwable?,
      extraMap: Map<String?, String?>?
  ) {
    producerListener?.onProducerFinishWithFailure(context.id, producerName, t, extraMap)
    producerListener2?.onProducerFinishWithFailure(context, producerName, t, extraMap)
  }

  override fun onProducerFinishWithCancellation(
      context: ProducerContext,
      producerName: String?,
      extraMap: Map<String?, String?>?
  ) {
    producerListener?.onProducerFinishWithCancellation(context.id, producerName, extraMap)
    producerListener2?.onProducerFinishWithCancellation(context, producerName, extraMap)
  }

  override fun onUltimateProducerReached(
      context: ProducerContext,
      producerName: String?,
      successful: Boolean
  ) {
    producerListener?.onUltimateProducerReached(context.id, producerName, successful)
    producerListener2?.onUltimateProducerReached(context, producerName, successful)
  }

  override fun requiresExtraMap(context: ProducerContext, producerName: String?): Boolean {
    var required: Boolean? = producerListener?.requiresExtraMap(context.id)
    if (required != true) {
      required = producerListener2?.requiresExtraMap(context, producerName)
    }
    return required ?: false
  }
}
