/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.MediaVariations;

import bolts.Continuation;
import bolts.Task;

/**
 * Disk cache read producer.
 *
 * <p>This producer looks in the disk cache for variations of the original image which hasn't been
 * found in cache itself.
 *
 * <p>If an alternative image is found, then it is passed to the consumer. If it's big enough for
 * the request's {@link ResizeOptions} then the request goes no further down the pipeline. If it's
 * smaller than required then it will be passed as a non-final response.
 *
 * <p>If the image is not found or is sent as non-final, then the request is passed to the next
 * producer in the sequence. Any results that the producer returns are passed to the consumer.
 *
 * <p>This producer is used only if the media variations experiment is turned on and does nothing
 * unless the image request includes defined {@link MediaVariations} and {@link ResizeOptions}.
 */
public class MediaVariationsFallbackProducer implements Producer<EncodedImage> {

  @VisibleForTesting static final String PRODUCER_NAME = "MediaVariationsFallbackProducer";
  public static final String EXTRA_CACHED_VALUE_FOUND = ProducerConstants.EXTRA_CACHED_VALUE_FOUND;
  public static final String EXTRA_CACHED_VALUE_USED_AS_LAST = "cached_value_used_as_last";

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final MediaVariationsIndex mMediaVariationsIndex;
  private final Producer<EncodedImage> mInputProducer;

  public MediaVariationsFallbackProducer(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      MediaVariationsIndex mediaVariationsIndex,
      Producer<EncodedImage> inputProducer) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mMediaVariationsIndex = mediaVariationsIndex;
    mInputProducer = inputProducer;
  }

  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final ResizeOptions resizeOptions = imageRequest.getResizeOptions();
    final MediaVariations mediaVariations = imageRequest.getMediaVariations();

    if (!imageRequest.isDiskCacheEnabled() ||
        resizeOptions == null ||
        resizeOptions.height <= 0 ||
        resizeOptions.width <= 0 ||
        mediaVariations == null) {
      startInputProducer(consumer, producerContext);
      return;
    }

    producerContext.getListener().onProducerStart(producerContext.getId(), PRODUCER_NAME);

    final AtomicBoolean isCancelled = new AtomicBoolean(false);

    if (mediaVariations.getVariants() != null) {
      chooseFromVariants(
          consumer,
          producerContext,
          mediaVariations,
          mediaVariations.getVariants(),
          imageRequest,
          resizeOptions,
          isCancelled);
    } else {
      Task<List<MediaVariations.Variant>> cachedVariantsTask =
          mMediaVariationsIndex.getCachedVariants(mediaVariations.getMediaId());
      cachedVariantsTask.continueWith(new Continuation<List<MediaVariations.Variant>, Object>() {

        @Override
        public Object then(Task<List<MediaVariations.Variant>> task) throws Exception {
          if (task.isCancelled() || task.isFaulted()) {
            return task;
          } else {
            try {
              if (task.getResult() == null || task.getResult().isEmpty()) {
                startInputProducer(consumer, producerContext);
                return null;
              } else {
                return chooseFromVariants(
                    consumer,
                    producerContext,
                    mediaVariations,
                    task.getResult(),
                    imageRequest,
                    resizeOptions,
                    isCancelled);
              }
            } catch (Exception e) {
              return null;
            }
          }
        }
      });
    }

    subscribeTaskForRequestCancellation(isCancelled, producerContext);
  }

  private Task chooseFromVariants(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext,
      final MediaVariations mediaVariations,
      final List<MediaVariations.Variant> variants,
      final ImageRequest imageRequest,
      final ResizeOptions resizeOptions,
      final AtomicBoolean isCancelled) {
    final BufferedDiskCache preferredCache =
        imageRequest.getCacheChoice() == ImageRequest.CacheChoice.SMALL ?
            mSmallImageBufferedDiskCache : mDefaultBufferedDiskCache;
    final Object callerContext = producerContext.getCallerContext();

    MediaVariations.Variant preferredVariant = null;
    CacheKey preferredCacheKey = null;
    Task<EncodedImage> diskLookupTask;

    for (int i = 0; i < variants.size(); i++) {
      final MediaVariations.Variant variant = variants.get(i);
      final CacheKey cacheKey =
          mCacheKeyFactory.getEncodedCacheKey(imageRequest, variant.getUri(), callerContext);
      if (preferredCache.containsSync(cacheKey)) {
        if (isCloserToRequest(variant, preferredVariant, resizeOptions)) {
          preferredVariant = variant;
          preferredCacheKey = cacheKey;
        }
      }
    }

    final boolean useAsLastResult;
    if (preferredCacheKey == null) {
      diskLookupTask = Task.forResult(null);
      useAsLastResult = false;
    } else {
      diskLookupTask = preferredCache.get(preferredCacheKey, isCancelled);
      useAsLastResult = !mediaVariations.shouldForceRequestForSpecifiedUri() &&
          isBigEnoughForRequestedSize(preferredVariant, resizeOptions);
    }

    Continuation<EncodedImage, Void> continuation =
        onFinishDiskReads(consumer, producerContext, useAsLastResult);
    return diskLookupTask.continueWith(continuation);
  }

  private static boolean isBigEnoughForRequestedSize(
      MediaVariations.Variant variant,
      ResizeOptions resizeOptions) {
    return variant.getWidth() >= resizeOptions.width && variant.getHeight() >= resizeOptions.height;
  }

  private static boolean isCloserToRequest(
      MediaVariations.Variant variant,
      MediaVariations.Variant comparison,
      ResizeOptions resizeOptions) {
    if (comparison == null) {
      return true;
    }
    if (isBigEnoughForRequestedSize(comparison, resizeOptions)) {
      return variant.getWidth() < comparison.getWidth() &&
          isBigEnoughForRequestedSize(variant, resizeOptions);
    } else {
      return variant.getWidth() > comparison.getWidth();
    }
  }

  private Continuation<EncodedImage, Void> onFinishDiskReads(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext,
      final boolean useAsLastResult) {
    final String requestId = producerContext.getId();
    final ProducerListener listener = producerContext.getListener();
    return new Continuation<EncodedImage, Void>() {
      @Override
      public Void then(Task<EncodedImage> task)
          throws Exception {
        final boolean triggerNextProducer;
        if (isTaskCancelled(task)) {
          listener.onProducerFinishWithCancellation(requestId, PRODUCER_NAME, null);
          consumer.onCancellation();
          triggerNextProducer = false;
        } else if (task.isFaulted()) {
          listener.onProducerFinishWithFailure(requestId, PRODUCER_NAME, task.getError(), null);
          startInputProducer(consumer, producerContext);
          triggerNextProducer = true;
        } else {
          EncodedImage cachedReference = task.getResult();
          if (cachedReference != null) {
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(listener, requestId, true, useAsLastResult));

            if (useAsLastResult) {
              consumer.onProgressUpdate(1);
            }
            consumer.onNewResult(cachedReference, useAsLastResult);
            cachedReference.close();

            triggerNextProducer = !useAsLastResult;
          } else {
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(listener, requestId, false, false));
            triggerNextProducer = true;
          }
        }

        if (triggerNextProducer) {
          startInputProducer(consumer, producerContext);
        }
        return null;
      }
    };
  }

  private void startInputProducer(
      Consumer<EncodedImage> consumer,
      ProducerContext producerContext) {
    mInputProducer
        .produceResults(new MediaVariationsConsumer(consumer, producerContext), producerContext);
  }

  private static boolean isTaskCancelled(Task<?> task) {
    return task.isCancelled() ||
        (task.isFaulted() && task.getError() instanceof CancellationException);
  }

  @VisibleForTesting
  static Map<String, String> getExtraMap(
      final ProducerListener listener,
      final String requestId,
      final boolean valueFound,
      boolean useAsLastResult) {
    if (!listener.requiresExtraMap(requestId)) {
      return null;
    }
    if (valueFound) {
      return ImmutableMap.of(
          EXTRA_CACHED_VALUE_FOUND,
          String.valueOf(true),
          EXTRA_CACHED_VALUE_USED_AS_LAST,
          String.valueOf(useAsLastResult));
    } else {
      return ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, String.valueOf(false));
    }
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

  @VisibleForTesting
  class MediaVariationsConsumer extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private final ProducerContext mProducerContext;

    public MediaVariationsConsumer(
        Consumer<EncodedImage> consumer, ProducerContext producerContext) {
      super(consumer);
      mProducerContext = producerContext;
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (isLast && newResult != null) {
        storeResultInDatabase(newResult);
      }
      getConsumer().onNewResult(newResult, isLast);
    }

    private void storeResultInDatabase(EncodedImage newResult) {
      final ImageRequest imageRequest = mProducerContext.getImageRequest();
      final MediaVariations mediaVariations = imageRequest.getMediaVariations();

      if (!imageRequest.isDiskCacheEnabled() ||
          mediaVariations == null) {
        return;
      }

      final CacheKey cacheKey =
          mCacheKeyFactory.getEncodedCacheKey(imageRequest, mProducerContext.getCallerContext());

      mMediaVariationsIndex
          .saveCachedVariant(mediaVariations.getMediaId(), cacheKey, newResult);
    }
  }
}
