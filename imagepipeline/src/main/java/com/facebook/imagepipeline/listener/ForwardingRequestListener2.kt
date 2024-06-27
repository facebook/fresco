/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener

import com.facebook.common.logging.FLog
import com.facebook.imagepipeline.producers.ProducerContext
import java.util.ArrayList

class ForwardingRequestListener2 : RequestListener2 {

  private val requestListeners: MutableList<RequestListener2>

  constructor(listenersToAdd: Set<RequestListener2?>?) {
    if (listenersToAdd == null) {
      requestListeners = mutableListOf()
      return
    }
    requestListeners = ArrayList(listenersToAdd.size)
    listenersToAdd.filterNotNullTo(requestListeners)
  }

  constructor(vararg listenersToAdd: RequestListener2?) {
    requestListeners = ArrayList(listenersToAdd.size)
    listenersToAdd.filterNotNullTo(requestListeners)
  }

  fun addRequestListener(requestListener: RequestListener2) {
    requestListeners.add(requestListener)
  }

  private inline fun forEachListener(methodName: String, block: (RequestListener2) -> Unit) {
    requestListeners.forEach {
      try {
        block(it)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        FLog.e(TAG, "InternalListener exception in $methodName", exception)
      }
    }
  }

  override fun onRequestStart(producerContext: ProducerContext) {
    forEachListener("onRequestStart") { it.onRequestStart(producerContext) }
  }

  override fun onProducerStart(producerContext: ProducerContext, producerName: String) {
    forEachListener("onProducerStart") { it.onProducerStart(producerContext, producerName) }
  }

  override fun onProducerFinishWithSuccess(
      producerContext: ProducerContext?,
      producerName: String?,
      extraMap: MutableMap<String, String>?
  ) {
    forEachListener("onProducerFinishWithSuccess") {
      it.onProducerFinishWithSuccess(producerContext, producerName, extraMap)
    }
  }

  override fun onProducerFinishWithFailure(
      producerContext: ProducerContext?,
      producerName: String?,
      t: Throwable?,
      extraMap: MutableMap<String, String>?
  ) {
    forEachListener("onProducerFinishWithFailure") {
      it.onProducerFinishWithFailure(producerContext, producerName, t, extraMap)
    }
  }

  override fun onProducerFinishWithCancellation(
      producerContext: ProducerContext?,
      producerName: String?,
      extraMap: MutableMap<String, String>?
  ) {
    forEachListener("onProducerFinishWithCancellation") {
      it.onProducerFinishWithCancellation(producerContext, producerName, extraMap)
    }
  }

  override fun onProducerEvent(
      producerContext: ProducerContext,
      producerName: String,
      producerEventName: String
  ) {
    forEachListener("onIntermediateChunkStart") {
      it.onProducerEvent(producerContext, producerName, producerEventName)
    }
  }

  override fun onUltimateProducerReached(
      producerContext: ProducerContext,
      producerName: String,
      successful: Boolean
  ) {
    forEachListener("onProducerFinishWithSuccess") {
      it.onUltimateProducerReached(producerContext, producerName, successful)
    }
  }

  override fun onRequestSuccess(producerContext: ProducerContext) {
    forEachListener("onRequestSuccess") { it.onRequestSuccess(producerContext) }
  }

  override fun onRequestFailure(producerContext: ProducerContext, throwable: Throwable) {
    forEachListener("onRequestFailure") { it.onRequestFailure(producerContext, throwable) }
  }

  override fun onRequestCancellation(producerContext: ProducerContext) {
    forEachListener("onRequestCancellation") { it.onRequestCancellation(producerContext) }
  }

  override fun requiresExtraMap(producerContext: ProducerContext, producerName: String): Boolean =
      requestListeners.any { it.requiresExtraMap(producerContext, producerName) }

  companion object {
    private const val TAG = "ForwardingRequestListener2"
  }
}
