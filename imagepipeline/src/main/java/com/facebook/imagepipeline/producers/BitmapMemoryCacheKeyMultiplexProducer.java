/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.util.Pair;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Multiplex producer that uses the bitmap memory cache key to combine requests.
 */
public class BitmapMemoryCacheKeyMultiplexProducer extends
    MultiplexProducer<Pair<CacheKey, ImageRequest.RequestLevel>,
        CloseableReference<CloseableImage>> {

  private final CacheKeyFactory mCacheKeyFactory;

  public BitmapMemoryCacheKeyMultiplexProducer(
      CacheKeyFactory cacheKeyFactory,
      Producer inputProducer) {
    super(inputProducer);
    mCacheKeyFactory = cacheKeyFactory;
  }

  protected Pair<CacheKey, ImageRequest.RequestLevel> getKey(
      ProducerContext producerContext) {
    return Pair.create(
        mCacheKeyFactory.getBitmapCacheKey(
            producerContext.getImageRequest(),
            producerContext.getCallerContext()),
        producerContext.getLowestPermittedRequestLevel());
  }

  public CloseableReference<CloseableImage> cloneOrNull(
      CloseableReference<CloseableImage> closeableImage) {
    return CloseableReference.cloneOrNull(closeableImage);
  }

}
