/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
      Producer nextProducer) {
    super(nextProducer);
    mCacheKeyFactory = cacheKeyFactory;
  }

  protected Pair<CacheKey, ImageRequest.RequestLevel> getKey(
      ProducerContext producerContext) {
    return Pair.create(
        mCacheKeyFactory.getBitmapCacheKey(producerContext.getImageRequest()),
        producerContext.getLowestPermittedRequestLevel());
  }

  public CloseableReference<CloseableImage> cloneOrNull(
      CloseableReference<CloseableImage> closeableImage) {
    return CloseableReference.cloneOrNull(closeableImage);
  }

}
