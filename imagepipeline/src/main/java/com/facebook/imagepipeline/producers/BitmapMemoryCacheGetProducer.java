/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.request.ImageRequest;

import com.facebook.common.internal.VisibleForTesting;

/**
 * Memory cache producer for the bitmap memory cache.
 */
public class BitmapMemoryCacheGetProducer
    extends MemoryCacheProducer<CacheKey, CloseableImage> {
  @VisibleForTesting static final String PRODUCER_NAME = "BitmapMemoryCacheGetProducer";

  public BitmapMemoryCacheGetProducer(
      MemoryCache<CacheKey, CloseableImage> memoryCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<CloseableReference<CloseableImage>> nextProducer) {
    super(memoryCache, cacheKeyFactory, nextProducer);
  }

  @Override
  protected CacheKey getCacheKey(ImageRequest imageRequest) {
    return mCacheKeyFactory.getBitmapCacheKey(imageRequest);
  }

  @Override
  protected boolean isResultFinal(
      CloseableReference<CloseableImage> cachedResultFound) {
    return cachedResultFound.get().getQualityInfo().isOfFullQuality();
  }

  @Override
  protected ImageRequest.RequestLevel getProducerRequestLevel() {
    return ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE;
  }

  @Override
  protected boolean shouldCacheReturnedValues() {
    return false;
  }

  @Override
  protected boolean shouldCacheResult(
      CloseableReference<CloseableImage> result,
      CacheKey cacheKey,
      boolean isLast) {
    return false;
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
