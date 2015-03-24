/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.cache.common.CacheKey;

/**
 * Multiplex producer that uses the encoded cache key to combine requests.
 */
public class EncodedCacheKeyMultiplexProducer extends
    MultiplexProducer<CacheKey, PooledByteBuffer> {

  private final CacheKeyFactory mCacheKeyFactory;

  public EncodedCacheKeyMultiplexProducer(CacheKeyFactory cacheKeyFactory, Producer nextProducer) {
    super(nextProducer);
    mCacheKeyFactory = cacheKeyFactory;
  }

  protected CacheKey getKey(ProducerContext producerContext) {
    return mCacheKeyFactory.getEncodedCacheKey(producerContext.getImageRequest());
  }
}
