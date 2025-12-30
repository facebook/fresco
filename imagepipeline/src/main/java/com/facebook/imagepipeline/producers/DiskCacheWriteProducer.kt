/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.Supplier
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.cache.CacheKeyFactory
import com.facebook.imagepipeline.core.DiskCachesStore
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.DiskCacheDecision.DiskCacheDecisionNoDiskCacheChosenException
import com.facebook.imagepipeline.producers.DiskCacheDecision.chooseDiskCacheForRequest
import com.facebook.imagepipeline.request.ImageRequest

/**
 * Disk cache write producer.
 *
 * This producer passes through to the next producer in the sequence, as long as the permitted
 * request level reaches beyond the disk cache. Otherwise this is a passive producer.
 *
 * The final result passed to the consumer put into the disk cache as well as being passed on.
 *
 * This implementation delegates disk cache requests to BufferedDiskCache.
 *
 * This producer is currently used only if the media variations experiment is turned on, to enable
 * another producer to sit between cache read and write.
 */
class DiskCacheWriteProducer(
    private val diskCachesStoreSupplier: Supplier<DiskCachesStore>,
    private val cacheKeyFactory: CacheKeyFactory,
    private val inputProducer: Producer<EncodedImage>,
) : Producer<EncodedImage> {
  override fun produceResults(consumer: Consumer<EncodedImage>, producerContext: ProducerContext) {
    maybeStartInputProducer(consumer, producerContext)
  }

  private fun maybeStartInputProducer(
      consumerOfDiskCacheWriteProducer: Consumer<EncodedImage>,
      producerContext: ProducerContext,
  ) {
    if (
        producerContext.lowestPermittedRequestLevel.getValue() >=
            ImageRequest.RequestLevel.DISK_CACHE.getValue()
    ) {
      producerContext.putOriginExtra("disk", "nil-result_write")
      consumerOfDiskCacheWriteProducer.onNewResult(null, Consumer.IS_LAST)
    } else {
      val consumer: Consumer<EncodedImage>?
      val isDiskCacheEnabledForWrite =
          producerContext.imageRequest.isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_WRITE)
      if (isDiskCacheEnabledForWrite) {
        consumer =
            DiskCacheWriteConsumer(
                consumerOfDiskCacheWriteProducer,
                producerContext,
                diskCachesStoreSupplier,
                cacheKeyFactory,
            )
      } else {
        consumer = consumerOfDiskCacheWriteProducer
      }

      inputProducer.produceResults(consumer, producerContext)
    }
  }

  /**
   * Consumer that consumes results from next producer in the sequence.
   *
   * The consumer puts the last result received into disk cache, and passes all results (success or
   * failure) down to the next consumer.
   */
  private class DiskCacheWriteConsumer(
      consumer: Consumer<EncodedImage>,
      private val producerContext: ProducerContext,
      private val diskCachesStoreSupplier: Supplier<DiskCachesStore>,
      private val cacheKeyFactory: CacheKeyFactory,
  ) : DelegatingConsumer<EncodedImage, EncodedImage>(consumer) {
    override fun onNewResultImpl(newResult: EncodedImage?, @Consumer.Status status: Int) {
      producerContext.producerListener.onProducerStart(producerContext, PRODUCER_NAME)
      // intermediate, null or uncacheable results are not cached, so we just forward them
      // as well as the images with unknown format which could be html response from the server
      if (
          isNotLast(status) ||
              newResult == null ||
              statusHasAnyFlag(
                  status,
                  Consumer.Companion.DO_NOT_CACHE_ENCODED or Consumer.Companion.IS_PARTIAL_RESULT,
              ) ||
              newResult.imageFormat === ImageFormat.UNKNOWN
      ) {
        producerContext.producerListener.onProducerFinishWithSuccess(
            producerContext,
            PRODUCER_NAME,
            null,
        )
        consumer.onNewResult(newResult, status)
        return
      }

      val imageRequest = producerContext.imageRequest
      val cacheKey = cacheKeyFactory.getEncodedCacheKey(imageRequest, producerContext.callerContext)
      val diskCachesStore = diskCachesStoreSupplier.get()
      val bufferedDiskCache =
          chooseDiskCacheForRequest(
              imageRequest,
              diskCachesStore.smallImageBufferedDiskCache,
              diskCachesStore.mainBufferedDiskCache,
              diskCachesStore.dynamicBufferedDiskCaches,
          )
      if (bufferedDiskCache == null) {
        producerContext.producerListener.onProducerFinishWithFailure(
            producerContext,
            PRODUCER_NAME,
            DiskCacheDecisionNoDiskCacheChosenException(
                "Got no disk cache for CacheChoice: " + imageRequest.cacheChoice.ordinal.toString()
            ),
            null,
        )
        consumer.onNewResult(newResult, status)
        return
      }
      bufferedDiskCache.put(cacheKey, newResult)
      producerContext.producerListener.onProducerFinishWithSuccess(
          producerContext,
          PRODUCER_NAME,
          null,
      )

      consumer.onNewResult(newResult, status)
    }
  }

  companion object {
    @VisibleForTesting const val PRODUCER_NAME: String = "DiskCacheWriteProducer"
  }
}
