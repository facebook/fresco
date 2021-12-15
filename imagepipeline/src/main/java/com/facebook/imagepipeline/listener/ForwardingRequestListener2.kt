/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener

import com.facebook.common.logging.FLog
import com.facebook.imagepipeline.producers.ProducerContext
import java.util.ArrayList
import java.util.Set

class ForwardingRequestListener2 : RequestListener2 {

  private var requestListeners: MutableList<RequestListener2>

  constructor(requestListeners: Set<RequestListener2?>) {
    this.requestListeners = ArrayList(requestListeners.size)
    for (requestListener in requestListeners) {
      if (requestListener != null) {
        this.requestListeners.add(requestListener)
      }
    }
  }

  constructor(vararg requestListeners: RequestListener2?) {
    this.requestListeners = ArrayList(requestListeners.size)
    for (requestListener in requestListeners) {
      if (requestListener != null) {
        this.requestListeners.add(requestListener)
      }
    }
  }

  fun addRequestListener(requestListener: RequestListener2) {
    requestListeners.add(requestListener)
  }

  override fun onRequestStart(producerContext: ProducerContext) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onRequestStart(producerContext)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onRequestStart", exception)
      }
    }
  }

  override fun onProducerStart(producerContext: ProducerContext, producerName: String) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onProducerStart(producerContext, producerName)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onProducerStart", exception)
      }
    }
  }

  override fun onProducerFinishWithSuccess(
      producerContext: ProducerContext?,
      producerName: String?,
      extraMap: MutableMap<String, String>?
  ) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onProducerFinishWithSuccess(producerContext, producerName, extraMap)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onProducerFinishWithSuccess", exception)
      }
    }
  }

  override fun onProducerFinishWithFailure(
      producerContext: ProducerContext,
      producerName: String,
      t: Throwable,
      extraMap: MutableMap<String, String>?
  ) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onProducerFinishWithFailure(producerContext, producerName, t, extraMap)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onProducerFinishWithFailure", exception)
      }
    }
  }

  override fun onProducerFinishWithCancellation(
      producerContext: ProducerContext,
      producerName: String,
      extraMap: MutableMap<String, String>?
  ) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onProducerFinishWithCancellation(producerContext, producerName, extraMap)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onProducerFinishWithCancellation", exception)
      }
    }
  }

  override fun onProducerEvent(
      producerContext: ProducerContext,
      producerName: String,
      producerEventName: String
  ) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onProducerEvent(producerContext, producerName, producerEventName)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onIntermediateChunkStart", exception)
      }
    }
  }

  override fun onUltimateProducerReached(
      producerContext: ProducerContext,
      producerName: String,
      successful: Boolean
  ) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onUltimateProducerReached(producerContext, producerName, successful)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onProducerFinishWithSuccess", exception)
      }
    }
  }

  override fun onRequestSuccess(producerContext: ProducerContext) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onRequestSuccess(producerContext)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onRequestSuccess", exception)
      }
    }
  }

  override fun onRequestFailure(producerContext: ProducerContext, throwable: Throwable) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onRequestFailure(producerContext, throwable)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onRequestFailure", exception)
      }
    }
  }

  override fun onRequestCancellation(producerContext: ProducerContext) {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      val listener = requestListeners[i]
      try {
        listener.onRequestCancellation(producerContext)
      } catch (exception: Exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onRequestCancellation", exception)
      }
    }
  }

  override fun requiresExtraMap(producerContext: ProducerContext, producerName: String): Boolean {
    val numberOfListeners = requestListeners.size
    for (i in 0 until numberOfListeners) {
      if (requestListeners[i].requiresExtraMap(producerContext, producerName)) {
        return true
      }
    }
    return false
  }

  private fun onException(message: String, t: Throwable) {
    FLog.e(TAG, message, t)
  }

  companion object {
    private const val TAG = "ForwardingRequestListener2"
  }
}
