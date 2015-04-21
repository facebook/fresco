/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import android.net.Uri;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Default implementation of {@link CacheKeyFactory}.
 */
public class DefaultCacheKeyFactory implements CacheKeyFactory {
  private static DefaultCacheKeyFactory sInstance = null;

  protected DefaultCacheKeyFactory() {
  }

  public static synchronized DefaultCacheKeyFactory getInstance() {
    if (sInstance == null) {
      sInstance = new DefaultCacheKeyFactory();
    }
    return sInstance;
  }

  @Override
  public CacheKey getBitmapCacheKey(ImageRequest request) {
    return new BitmapMemoryCacheKey(
        getCacheKeySourceUri(request.getSourceUri()).toString(),
        request.getResizeOptions(),
        request.getAutoRotateEnabled(),
        request.getImageDecodeOptions());
  }

  @Override
  public CacheKey getEncodedCacheKey(ImageRequest request) {
    return new SimpleCacheKey(getCacheKeySourceUri(request.getSourceUri()).toString());
  }

  @Override
  public Uri getCacheKeySourceUri(Uri sourceUri) {
    return sourceUri;
  }
}
