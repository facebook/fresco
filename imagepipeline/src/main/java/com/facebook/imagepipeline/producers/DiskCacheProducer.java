/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

import bolts.Continuation;
import bolts.Task;

/**
 * Disk cache producer.
 *
 * <p>This producer looks in the disk cache for the requested image. If the image is found, then it
 * is passed to the consumer. If the image is not found, then the request is passed to the next
 * producer in the sequence. Any results that the producer returns are passed to the consumer, and
 * the last result is also put into the disk cache.
 *
 * <p>This implementation delegates disk cache requests to BufferedDiskCache.
 */
public class DiskCacheProducer implements Producer<EncodedImage> {
  @VisibleForTesting static final String PRODUCER_NAME = "DiskCacheProducer";
  @VisibleForTesting static final String VALUE_FOUND = "cached_value_found";

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<EncodedImage> mInputProducer;
  private final boolean mChooseCacheByImageSize;
  private final int mForceSmallCacheThresholdBytes;

  public DiskCacheProducer(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<EncodedImage> inputProducer,
      int forceSmallCacheThresholdBytes) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
    mForceSmallCacheThresholdBytes = forceSmallCacheThresholdBytes;
    mChooseCacheByImageSize = (forceSmallCacheThresholdBytes > 0);
  }

  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {
    ImageRequest imageRequest = producerContext.getImageRequest();
    if (!imageRequest.isDiskCacheEnabled()) {
      maybeStartInputProducer(consumer, consumer, producerContext);
      return;
    }

    producerContext.getListener().onProducerStart(producerContext.getId(), PRODUCER_NAME);

    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest);
    boolean isSmallRequest = (imageRequest.getImageType() == ImageRequest.ImageType.SMALL);
    final BufferedDiskCache preferredCache = isSmallRequest ?
        mSmallImageBufferedDiskCache : mDefaultBufferedDiskCache;
    final AtomicBoolean isCancelled = new AtomicBoolean(false);
    Task<EncodedImage> diskLookupTask;
    if (mChooseCacheByImageSize) {
      boolean alreadyInSmall = mSmallImageBufferedDiskCache.containsSync(cacheKey);
      boolean alreadyInMain = mDefaultBufferedDiskCache.containsSync(cacheKey);
      final BufferedDiskCache firstCache;
      final BufferedDiskCache secondCache ;
      if (alreadyInSmall || !alreadyInMain) {
        firstCache = mSmallImageBufferedDiskCache;
        secondCache = mDefaultBufferedDiskCache;
      } else {
        firstCache = mDefaultBufferedDiskCache;
        secondCache = mSmallImageBufferedDiskCache;
      }
      diskLookupTask = firstCache.get(cacheKey, isCancelled);
      diskLookupTask = diskLookupTask.continueWithTask(
          new Continuation<EncodedImage, Task<EncodedImage>>() {
        @Override
        public Task<EncodedImage> then(Task<EncodedImage> task) throws Exception {
          if (isTaskCancelled(task) || (!task.isFaulted() && task.getResult() != null)) {
            return task;
          }
          return secondCache.get(cacheKey, isCancelled);
        }
      });
    } else {
      diskLookupTask = preferredCache.get(cacheKey, isCancelled);
    }
    Continuation<EncodedImage, Void> continuation =
        onFinishDiskReads(consumer, preferredCache, cacheKey, producerContext);
    diskLookupTask.continueWith(continuation);
    subscribeTaskForRequestCancellation(isCancelled, producerContext);
  }

  private Continuation<EncodedImage, Void> onFinishDiskReads(
      final Consumer<EncodedImage> consumer,
      final BufferedDiskCache preferredCache,
      final CacheKey preferredCacheKey,
      final ProducerContext producerContext) {
    final String requestId = producerContext.getId();
    final ProducerListener listener = producerContext.getListener();
    return new Continuation<EncodedImage, Void>() {
      @Override
      public Void then(Task<EncodedImage> task)
          throws Exception {
        if (isTaskCancelled(task)) {
          listener.onProducerFinishWithCancellation(requestId, PRODUCER_NAME, null);
          consumer.onCancellation();
        } else if (task.isFaulted()) {
          listener.onProducerFinishWithFailure(requestId, PRODUCER_NAME, task.getError(), null);
          maybeStartInputProducer(
              consumer,
              new DiskCacheConsumer(consumer, preferredCache, preferredCacheKey),
              producerContext);
        } else {
          EncodedImage cachedReference = task.getResult();
          if (cachedReference != null) {
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(listener, requestId, true));
            consumer.onProgressUpdate(1);
            consumer.onNewResult(cachedReference, true);
            cachedReference.close();
          } else {
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(listener, requestId, false));
            maybeStartInputProducer(
                consumer,
                new DiskCacheConsumer(consumer, preferredCache, preferredCacheKey),
                producerContext);
          }
        }
        return null;
      }
    };
  }

  private static boolean isTaskCancelled(Task<?> task) {
    return task.isCancelled() ||
        (task.isFaulted() && task.getError() instanceof CancellationException);
  }

  private void maybeStartInputProducer(
      Consumer<EncodedImage> consumerOfDiskCacheProducer,
      Consumer<EncodedImage> consumerOfInputProducer,
      ProducerContext producerContext) {
    if (producerContext.getLowestPermittedRequestLevel().getValue() >=
        ImageRequest.RequestLevel.DISK_CACHE.getValue()) {
      consumerOfDiskCacheProducer.onNewResult(null, true);
      return;
    }

    mInputProducer.produceResults(consumerOfInputProducer, producerContext);
  }

  @VisibleForTesting
  static Map<String, String> getExtraMap(
      final ProducerListener listener,
      final String requestId,
      final boolean valueFound) {
    if (!listener.requiresExtraMap(requestId)) {
      return null;
    }
    return ImmutableMap.of(VALUE_FOUND, String.valueOf(valueFound));
  }

  private void subscribeTaskForRequestCancellation(
      final AtomicBoolean isCancelled,
      ProducerContext producerContext) {
    producerContext.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            isCancelled.set(true);
          }
        });
  }

  /**
   * Consumer that consumes results from next producer in the sequence.
   *
   * <p>The consumer puts the last result received into disk cache, and passes all results (success
   * or failure) down to the next consumer.
   */
  private class DiskCacheConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final BufferedDiskCache mCache;
    private final CacheKey mCacheKey;

    private DiskCacheConsumer(
        final Consumer<EncodedImage> consumer,
        final BufferedDiskCache cache,
        final CacheKey cacheKey) {
      super(consumer);
      mCache = cache;
      mCacheKey = cacheKey;
    }

    @Override
    public void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (newResult != null && isLast) {
        if (mChooseCacheByImageSize) {
          int size = newResult.getSize();
          if (size > 0 && size < mForceSmallCacheThresholdBytes) {
            mSmallImageBufferedDiskCache.put(mCacheKey, newResult);
          } else {
            mDefaultBufferedDiskCache.put(mCacheKey, newResult);
          }
        } else {
          mCache.put(mCacheKey, newResult);
        }
      }
      getConsumer().onNewResult(newResult, isLast);
    }
  }
}
