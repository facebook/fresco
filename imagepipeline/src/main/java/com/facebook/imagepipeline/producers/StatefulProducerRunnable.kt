/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.producers

import com.facebook.common.executors.StatefulRunnable
import java.lang.Exception

/**
 * [StatefulRunnable] intended to be used by producers.
 *
 * Class implements common functionality related to handling producer instrumentation and resource
 * management.
 */
abstract class StatefulProducerRunnable<T>(
    private val consumer: Consumer<T>,
    private val producerListener: ProducerListener2,
    private val producerContext: ProducerContext,
    private val producerName: String
) : StatefulRunnable<T>() {
  override fun onSuccess(result: T?) {
    producerListener.onProducerFinishWithSuccess(
        producerContext,
        producerName,
        if (producerListener.requiresExtraMap(producerContext, producerName))
            getExtraMapOnSuccess(result)
        else null)
    consumer.onNewResult(result, Consumer.IS_LAST)
  }

  override fun onFailure(e: Exception) {
    producerListener.onProducerFinishWithFailure(
        producerContext,
        producerName,
        e,
        if (producerListener.requiresExtraMap(producerContext, producerName))
            getExtraMapOnFailure(e)
        else null)
    consumer.onFailure(e)
  }

  override fun onCancellation() {
    producerListener.onProducerFinishWithCancellation(
        producerContext,
        producerName,
        if (producerListener.requiresExtraMap(producerContext, producerName)) extraMapOnCancellation
        else null)
    consumer.onCancellation()
  }

  /** Create extra map for result */
  protected open fun getExtraMapOnSuccess(result: T?): Map<String?, String?>? {
    return null
  }

  /** Create extra map for exception */
  protected open fun getExtraMapOnFailure(exception: Exception?): Map<String, String>? {
    return null
  }

  /** Create extra map for cancellation */
  protected open val extraMapOnCancellation: Map<String, String>?
    get() = null

  abstract override fun disposeResult(result: T?)

  init {
    producerListener.onProducerStart(producerContext, producerName)
  }
}
