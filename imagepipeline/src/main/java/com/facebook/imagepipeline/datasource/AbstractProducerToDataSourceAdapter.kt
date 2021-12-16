/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource

import com.facebook.common.internal.Preconditions
import com.facebook.datasource.AbstractDataSource
import com.facebook.imagepipeline.listener.RequestListener2
import com.facebook.imagepipeline.producers.BaseConsumer
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.Producer
import com.facebook.imagepipeline.producers.ProducerContext
import com.facebook.imagepipeline.producers.SettableProducerContext
import com.facebook.imagepipeline.request.HasImageRequest
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.systrace.FrescoSystrace.beginSection
import com.facebook.imagepipeline.systrace.FrescoSystrace.endSection
import com.facebook.imagepipeline.systrace.FrescoSystrace.isTracing
import javax.annotation.concurrent.ThreadSafe

/**
 * DataSource<T> backed by a Producer<T>
 *
 * @param <T> </T></T></T>
 */
@ThreadSafe
abstract class AbstractProducerToDataSourceAdapter<T>
protected constructor(
    producer: Producer<T>,
    settableProducerContext: SettableProducerContext,
    requestListener: RequestListener2
) : AbstractDataSource<T>(), HasImageRequest {

  private val settableProducerContext: SettableProducerContext
  private val requestListener: RequestListener2

  private fun createConsumer(): Consumer<T> {
    return object : BaseConsumer<T>() {
      override fun onNewResultImpl(newResult: T?, @Consumer.Status status: Int) {
        this@AbstractProducerToDataSourceAdapter.onNewResultImpl(
            newResult, status, settableProducerContext)
      }

      override fun onFailureImpl(throwable: Throwable) {
        this@AbstractProducerToDataSourceAdapter.onFailureImpl(throwable)
      }

      override fun onCancellationImpl() {
        this@AbstractProducerToDataSourceAdapter.onCancellationImpl()
      }

      override fun onProgressUpdateImpl(progress: Float) {
        setProgress(progress)
      }
    }
  }

  protected open fun onNewResultImpl(result: T?, status: Int, producerContext: ProducerContext) {
    val isLast = BaseConsumer.isLast(status)
    if (super.setResult(result, isLast, getExtras(producerContext))) {
      if (isLast) {
        requestListener.onRequestSuccess(settableProducerContext)
      }
    }
  }

  protected fun getExtras(producerContext: ProducerContext): Map<String, Any> {
    return producerContext.extras
  }

  private fun onFailureImpl(throwable: Throwable) {
    if (super.setFailure(throwable, getExtras(settableProducerContext))) {
      requestListener.onRequestFailure(settableProducerContext, throwable)
    }
  }

  @Synchronized
  private fun onCancellationImpl() {
    Preconditions.checkState(isClosed)
  }

  override fun getImageRequest(): ImageRequest? {
    return settableProducerContext.imageRequest
  }

  override fun close(): Boolean {
    if (!super.close()) {
      return false
    }
    if (!super.isFinished()) {
      requestListener.onRequestCancellation(settableProducerContext)
      settableProducerContext.cancel()
    }
    return true
  }

  private fun setInitialExtras() {
    extras = settableProducerContext.extras
  }

  init {
    if (isTracing()) {
      beginSection("AbstractProducerToDataSourceAdapter()")
    }
    this.settableProducerContext = settableProducerContext
    this.requestListener = requestListener
    setInitialExtras()
    if (isTracing()) {
      beginSection("AbstractProducerToDataSourceAdapter()->onRequestStart")
    }
    requestListener.onRequestStart(settableProducerContext)
    if (isTracing()) {
      endSection()
    }
    if (isTracing()) {
      beginSection("AbstractProducerToDataSourceAdapter()->produceResult")
    }
    producer.produceResults(createConsumer(), settableProducerContext)
    if (isTracing()) {
      endSection()
    }
    if (isTracing()) {
      endSection()
    }
  }
}
