/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

import bolts.Task;

/**
 * Task factory for the simple disk cache case of attempting to load the image from whichever cache
 * is requested by the image request.
 */
public class SmallCacheIfRequestedDiskCachePolicy
    implements DiskCachePolicy {

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;

  public SmallCacheIfRequestedDiskCachePolicy(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache, CacheKeyFactory cacheKeyFactory) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
  }

  @Override
  public Task<EncodedImage> createAndStartCacheReadTask(
      ImageRequest imageRequest,
      Object callerContext,
      AtomicBoolean isCancelled) {
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, callerContext);
    if (imageRequest.getCacheChoice() == ImageRequest.CacheChoice.SMALL) {
      return mSmallImageBufferedDiskCache.get(cacheKey, isCancelled);
    } else {
      return mDefaultBufferedDiskCache.get(cacheKey, isCancelled);
    }
  }

  @Override
  public void writeToCache(
      EncodedImage newResult,
      ImageRequest imageRequest,
      Object callerContext) {
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, callerContext);

    if (imageRequest.getCacheChoice() == ImageRequest.CacheChoice.SMALL) {
      mSmallImageBufferedDiskCache.put(cacheKey, newResult);
    } else {
      mDefaultBufferedDiskCache.put(cacheKey, newResult);
    }
  }
}
