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

import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Multiplex producer that uses the encoded cache key to combine requests.
 */
public class EncodedCacheKeyMultiplexProducer extends
    MultiplexProducer<Pair<CacheKey, ImageRequest.RequestLevel>, EncodedImage> {

  private final CacheKeyFactory mCacheKeyFactory;
  private final boolean mUseNewInterface;

  public EncodedCacheKeyMultiplexProducer(
      CacheKeyFactory cacheKeyFactory,
      Producer inputProducer,
      boolean useNewInterface) {
    super(inputProducer);
    mCacheKeyFactory = cacheKeyFactory;
    mUseNewInterface = useNewInterface;
  }

  protected Pair<CacheKey, ImageRequest.RequestLevel> getKey(ProducerContext producerContext) {
    CacheKey cacheKey = mUseNewInterface ?
        mCacheKeyFactory.getEncodedCacheKeys(producerContext.getImageRequest()).get(0) :
        mCacheKeyFactory.getEncodedCacheKey(producerContext.getImageRequest());

    return Pair.create(
        cacheKey,
        producerContext.getLowestPermittedRequestLevel());
  }

  public EncodedImage cloneOrNull(EncodedImage encodedImage) {
    return EncodedImage.cloneOrNull(encodedImage);
  }
}
