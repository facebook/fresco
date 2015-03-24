/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Multiplex producer that uses the bitmap memory cache key to combine requests.
 */
public class BitmapMemoryCacheKeyMultiplexProducer extends
    MultiplexProducer<BitmapMemoryCacheKey, CloseableImage> {

  private final CacheKeyFactory mCacheKeyFactory;

  public BitmapMemoryCacheKeyMultiplexProducer(
      CacheKeyFactory cacheKeyFactory,
      Producer nextProducer) {
    super(nextProducer);
    mCacheKeyFactory = cacheKeyFactory;
  }

  protected BitmapMemoryCacheKey getKey(ProducerContext producerContext) {
    return mCacheKeyFactory.getBitmapCacheKey(producerContext.getImageRequest());
  }

}
