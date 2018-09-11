/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.util.Pair;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Multiplex producer that uses the encoded cache key to combine requests.
 */
public class EncodedCacheKeyMultiplexProducer extends
    MultiplexProducer<Pair<CacheKey, ImageRequest.RequestLevel>, EncodedImage> {

  private final CacheKeyFactory mCacheKeyFactory;

  public EncodedCacheKeyMultiplexProducer(
      CacheKeyFactory cacheKeyFactory,
      Producer inputProducer) {
    super(inputProducer);
    mCacheKeyFactory = cacheKeyFactory;
  }

  protected Pair<CacheKey, ImageRequest.RequestLevel> getKey(ProducerContext producerContext) {
    return Pair.create(
        mCacheKeyFactory.getEncodedCacheKey(
            producerContext.getImageRequest(),
            producerContext.getCallerContext()),
        producerContext.getLowestPermittedRequestLevel());
  }

  public EncodedImage cloneOrNull(EncodedImage encodedImage) {
    return EncodedImage.cloneOrNull(encodedImage);
  }
}
