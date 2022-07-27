/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.EncodedImage

/**
 * Remove image transform meta data producer
 *
 * Remove the [ImageTransformMetaData] object from the results passed down from the next producer,
 * and adds it to the result that it returns to the consumer.
 */
class RemoveImageTransformMetaDataProducer(private val inputProducer: Producer<EncodedImage?>) :
    Producer<CloseableReference<PooledByteBuffer>> {

  override fun produceResults(
      consumer: Consumer<CloseableReference<PooledByteBuffer>>,
      context: ProducerContext
  ) {
    inputProducer.produceResults(RemoveImageTransformMetaDataConsumer(consumer), context)
  }

  private inner class RemoveImageTransformMetaDataConsumer(
      consumer: Consumer<CloseableReference<PooledByteBuffer>>
  ) : DelegatingConsumer<EncodedImage?, CloseableReference<PooledByteBuffer>>(consumer) {
    override fun onNewResultImpl(newResult: EncodedImage?, @Consumer.Status status: Int) {
      var ret: CloseableReference<PooledByteBuffer>? = null
      try {
        if (EncodedImage.isValid(newResult)) {
          ret = newResult?.byteBufferRef
        }
        consumer.onNewResult(ret, status)
      } finally {
        CloseableReference.closeSafely(ret)
      }
    }
  }
}
