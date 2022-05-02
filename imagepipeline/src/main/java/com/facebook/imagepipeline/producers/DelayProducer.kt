/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.CloseableImage
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * A ScheduledExecutorService is a significant dependency and we do not want to require it. If not
 * provided, this producer is a no-op.
 */
class DelayProducer(
    private val inputProducer: Producer<CloseableReference<CloseableImage>>,
    private val backgroundTasksExecutor: ScheduledExecutorService?
) : Producer<CloseableReference<CloseableImage>> {
  override fun produceResults(
      consumer: Consumer<CloseableReference<CloseableImage>>,
      context: ProducerContext
  ) {
    val request = context.imageRequest
    if (backgroundTasksExecutor != null) {
      backgroundTasksExecutor.schedule(
          Runnable { inputProducer.produceResults(consumer, context) },
          request.delayMs.toLong(),
          TimeUnit.MILLISECONDS)
    } else {
      inputProducer.produceResults(consumer, context)
    }
  }
}
