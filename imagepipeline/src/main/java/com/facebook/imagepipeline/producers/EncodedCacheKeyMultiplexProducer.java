/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.util.Pair;
import com.facebook.cache.common.CacheKey;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Multiplex producer that uses the encoded cache key to combine requests. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class EncodedCacheKeyMultiplexProducer
    extends MultiplexProducer<Pair<CacheKey, ImageRequest.RequestLevel>, EncodedImage> {

  private final CacheKeyFactory mCacheKeyFactory;

  public EncodedCacheKeyMultiplexProducer(
      CacheKeyFactory cacheKeyFactory,
      boolean keepCancelledFetchAsLowPriority,
      Producer inputProducer) {
    super(
        inputProducer,
        "EncodedCacheKeyMultiplexProducer",
        HasExtraData.KEY_MULTIPLEX_ENCODED_COUNT,
        keepCancelledFetchAsLowPriority);
    mCacheKeyFactory = cacheKeyFactory;
  }

  protected Pair<CacheKey, ImageRequest.RequestLevel> getKey(ProducerContext producerContext) {
    return Pair.create(
        mCacheKeyFactory.getEncodedCacheKey(
            producerContext.getImageRequest(), producerContext.getCallerContext()),
        producerContext.getLowestPermittedRequestLevel());
  }

  public @Nullable EncodedImage cloneOrNull(@Nullable EncodedImage encodedImage) {
    return EncodedImage.cloneOrNull(encodedImage);
  }
}
