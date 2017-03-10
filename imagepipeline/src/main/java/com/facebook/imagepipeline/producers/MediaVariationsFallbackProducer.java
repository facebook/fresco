/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.DiskCachePolicy;
import com.facebook.imagepipeline.cache.MediaIdExtractor;
import com.facebook.imagepipeline.cache.MediaVariationsIndex;
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

  public static final String PRODUCER_NAME = "MediaVariationsFallbackProducer";
  public static final String EXTRA_CACHED_VALUE_FOUND = ProducerConstants.EXTRA_CACHED_VALUE_FOUND;
  public static final String EXTRA_CACHED_VALUE_USED_AS_LAST = "cached_value_used_as_last";
  public static final String EXTRA_VARIANTS_COUNT = "variants_count";
  public static final String EXTRA_VARIANTS_SOURCE = "variants_source";

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final MediaVariationsIndex mMediaVariationsIndex;
  @Nullable private MediaIdExtractor mMediaIdExtractor;
  private final DiskCachePolicy mDiskCachePolicy;
  private final Producer<EncodedImage> mInputProducer;

  public MediaVariationsFallbackProducer(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      MediaVariationsIndex mediaVariationsIndex,
      @Nullable MediaIdExtractor mediaIdExtractor,
      DiskCachePolicy diskCachePolicy,
      Producer<EncodedImage> inputProducer) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mMediaVariationsIndex = mediaVariationsIndex;
    mMediaIdExtractor = mediaIdExtractor;
    mDiskCachePolicy = diskCachePolicy;
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
        resizeOptions.width <= 0) {
      startInputProducerWithExistingConsumer(consumer, producerContext);
      return;
    }

    final String mediaId;
    final @MediaVariations.Source String source;
    if (mediaVariations == null) {
      if (mMediaIdExtractor == null) {
        mediaId = null;
        source = null;
      } else {
        mediaId = mMediaIdExtractor.getMediaIdFrom(imageRequest.getSourceUri());
        source = MediaVariations.SOURCE_ID_EXTRACTOR;
      }
    } else {
      mediaId = mediaVariations.getMediaId();
      source = MediaVariations.SOURCE_INDEX_DB;
    }

    if (mediaVariations == null && mediaId == null) {
      startInputProducerWithExistingConsumer(consumer, producerContext);
      return;
    }

    producerContext.getListener().onProducerStart(producerContext.getId(), PRODUCER_NAME);

    final AtomicBoolean isCancelled = new AtomicBoolean(false);

    if (mediaVariations != null && mediaVariations.getVariantsCount() > 0) {
      chooseFromVariants(
          consumer,
          producerContext,
          imageRequest,
          mediaVariations,
          resizeOptions,
          isCancelled);
    } else {
      MediaVariations.Builder mediaVariationsBuilder =
          MediaVariations.newBuilderForMediaId(mediaId)
              .setForceRequestForSpecifiedUri(
                  mediaVariations != null && mediaVariations.shouldForceRequestForSpecifiedUri())
              .setSource(source);
      Task<MediaVariations> indexedMediaVariationsTask = mMediaVariationsIndex
          .getCachedVariants(mediaId, mediaVariationsBuilder);
      indexedMediaVariationsTask.continueWith(new Continuation<MediaVariations, Object>() {

        @Override
        public Object then(Task<MediaVariations> task) throws Exception {
          if (task.isCancelled() || task.isFaulted()) {
            return task;
          } else {
            try {
              if (task.getResult() == null) {
                startInputProducerWithWrappedConsumer(consumer, producerContext, mediaId);
                return null;
              } else {
                return chooseFromVariants(
                    consumer,
                    producerContext,
                    imageRequest,
                    task.getResult(),
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
      final ImageRequest imageRequest,
      final MediaVariations mediaVariations,
      final ResizeOptions resizeOptions,
      final AtomicBoolean isCancelled) {

    if (mediaVariations.getVariantsCount() == 0) {
      Continuation<EncodedImage, Void> continuation = onFinishDiskReads(
          consumer,
          producerContext,
          imageRequest,
          mediaVariations,
          Collections.<MediaVariations.Variant>emptyList(),
          0,
          isCancelled);
      return Task.forResult((EncodedImage) null).continueWith(continuation);
    }

    List<MediaVariations.Variant> sortedVariants =
        mediaVariations.getSortedVariants(new VariantComparator(resizeOptions));

    return attemptCacheReadForVariant(
        consumer,
        producerContext,
        imageRequest,
        mediaVariations,
        sortedVariants,
        0,
        isCancelled);
  }

  private Task attemptCacheReadForVariant(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext,
      ImageRequest imageRequest,
      MediaVariations mediaVariations,
      List<MediaVariations.Variant> sortedVariants,
      int index,
      AtomicBoolean isCancelled) {
    final MediaVariations.Variant variant = sortedVariants.get(index);
    final CacheKey cacheKey = mCacheKeyFactory
        .getEncodedCacheKey(imageRequest, variant.getUri(), producerContext.getCallerContext());
    final ImageRequest.CacheChoice cacheChoice;
    if (variant.getCacheChoice() == null) {
      cacheChoice = imageRequest.getCacheChoice();
    } else {
      cacheChoice = variant.getCacheChoice();
    }
    final BufferedDiskCache preferredCache = cacheChoice == ImageRequest.CacheChoice.SMALL
        ? mSmallImageBufferedDiskCache
        : mDefaultBufferedDiskCache;

    Task<EncodedImage> readTask = preferredCache.get(cacheKey, isCancelled);

    Continuation<EncodedImage, Void> continuation = onFinishDiskReads(
        consumer,
        producerContext,
        imageRequest,
        mediaVariations,
        sortedVariants,
        index,
        isCancelled);
    return readTask.continueWith(continuation);
  }

  private static boolean isBigEnoughForRequestedSize(
      MediaVariations.Variant variant,
      ResizeOptions resizeOptions) {
    return variant.getWidth() >= resizeOptions.width && variant.getHeight() >= resizeOptions.height;
  }

  private Continuation<EncodedImage, Void> onFinishDiskReads(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext,
      final ImageRequest imageRequest,
      final MediaVariations mediaVariations,
      final List<MediaVariations.Variant> sortedVariants,
      final int variantsIndex,
      final AtomicBoolean isCancelled) {
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
          startInputProducerWithWrappedConsumer(
              consumer,
              producerContext,
              mediaVariations.getMediaId());
          triggerNextProducer = true;
        } else {
          EncodedImage cachedReference = task.getResult();
          if (cachedReference != null) {
            final boolean useAsLastResult = !mediaVariations.shouldForceRequestForSpecifiedUri() &&
                isBigEnoughForRequestedSize(
                    sortedVariants.get(variantsIndex),
                    imageRequest.getResizeOptions());
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(
                    listener,
                    requestId,
                    true,
                    sortedVariants.size(),
                    mediaVariations.getSource(),
                    useAsLastResult));

            if (useAsLastResult) {
              consumer.onProgressUpdate(1);
            }
            consumer.onNewResult(cachedReference, useAsLastResult);
            cachedReference.close();

            triggerNextProducer = !useAsLastResult;
          } else if (variantsIndex < sortedVariants.size() - 1) {
            // TODO t14487493: Remove the item from the index

            attemptCacheReadForVariant(
                consumer,
                producerContext,
                imageRequest,
                mediaVariations,
                sortedVariants,
                variantsIndex + 1,
                isCancelled);

            triggerNextProducer = false;
          } else {
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(
                    listener,
                    requestId,
                    false,
                    sortedVariants.size(),
                    mediaVariations.getSource(),
                    false));
            triggerNextProducer = true;
          }
        }

        if (triggerNextProducer) {
          startInputProducerWithWrappedConsumer(
              consumer,
              producerContext,
              mediaVariations.getMediaId());
        }
        return null;
      }
    };
  }

  private void startInputProducerWithExistingConsumer(
      Consumer<EncodedImage> consumer,
      ProducerContext producerContext) {
    mInputProducer.produceResults(consumer, producerContext);
  }

  private void startInputProducerWithWrappedConsumer(
      Consumer<EncodedImage> consumer,
      ProducerContext producerContext,
      String mediaId) {
    mInputProducer.produceResults(
        new MediaVariationsConsumer(consumer, producerContext, mediaId),
        producerContext);
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
      final int variantsCount,
      final String variantsSource,
      boolean useAsLastResult) {
    if (!listener.requiresExtraMap(requestId)) {
      return null;
    }
    if (valueFound) {
      return ImmutableMap.of(
          EXTRA_CACHED_VALUE_FOUND,
          String.valueOf(true),
          EXTRA_CACHED_VALUE_USED_AS_LAST,
          String.valueOf(useAsLastResult),
          EXTRA_VARIANTS_COUNT,
          String.valueOf(variantsCount),
          EXTRA_VARIANTS_SOURCE,
          variantsSource);
    } else {
      return ImmutableMap.of(
          EXTRA_CACHED_VALUE_FOUND,
          String.valueOf(false),
          EXTRA_VARIANTS_COUNT,
          String.valueOf(variantsCount),
          EXTRA_VARIANTS_SOURCE,
          variantsSource);
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
    private final String mMediaId;

    public MediaVariationsConsumer(
        Consumer<EncodedImage> consumer, ProducerContext producerContext, String mediaId) {
      super(consumer);
      mProducerContext = producerContext;
      mMediaId = mediaId;
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

      if (!imageRequest.isDiskCacheEnabled() || mMediaId == null) {
        return;
      }

      final ImageRequest.CacheChoice cacheChoice =
          mDiskCachePolicy.getCacheChoiceForResult(imageRequest, newResult);
      final CacheKey cacheKey =
          mCacheKeyFactory.getEncodedCacheKey(imageRequest, mProducerContext.getCallerContext());

      mMediaVariationsIndex.saveCachedVariant(mMediaId, cacheChoice, cacheKey, newResult);
    }
  }

  @VisibleForTesting
  static class VariantComparator implements Comparator<MediaVariations.Variant> {

    private final ResizeOptions mResizeOptions;

    VariantComparator(ResizeOptions resizeOptions) {
      mResizeOptions = resizeOptions;
    }

    @Override
    public int compare(MediaVariations.Variant o1, MediaVariations.Variant o2) {
      final boolean o1BigEnough = isBigEnoughForRequestedSize(o1, mResizeOptions);
      final boolean o2BigEnough = isBigEnoughForRequestedSize(o2, mResizeOptions);

      if (o1BigEnough && o2BigEnough) {
        // Prefer the smaller image as both are bigger than needed
        return o1.getWidth() - o2.getWidth();
      } else if (o1BigEnough) {
        return -1;
      } else if (o2BigEnough) {
        return 1;
      } else {
        // Prefer the larger image as both are smaller than needed
        return o2.getWidth() - o1.getWidth();
      }
    }
  }
}
