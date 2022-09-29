/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource

import com.facebook.datasource.DataSource
import com.facebook.imagepipeline.listener.RequestListener2
import com.facebook.imagepipeline.producers.Producer
import com.facebook.imagepipeline.producers.SettableProducerContext
import javax.annotation.concurrent.ThreadSafe

/**
 * DataSource<T> backed by a Producer<T>
 *
 * @param <T> </T></T></T>
 */
@ThreadSafe
class ProducerToDataSourceAdapter<T>
private constructor(
    producer: Producer<T>,
    settableProducerContext: SettableProducerContext,
    listener: RequestListener2
) : AbstractProducerToDataSourceAdapter<T>(producer, settableProducerContext, listener) {
  companion object {
    @JvmStatic
    fun <T> create(
        producer: Producer<T>,
        settableProducerContext: SettableProducerContext,
        listener: RequestListener2
    ): DataSource<T> = ProducerToDataSourceAdapter(producer, settableProducerContext, listener)
  }
}
