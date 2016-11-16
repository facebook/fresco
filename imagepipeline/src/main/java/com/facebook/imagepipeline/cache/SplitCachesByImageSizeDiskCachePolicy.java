/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

import bolts.Continuation;
import bolts.Task;

/**
 * Task factory to attempt to load an image from either the main or small disk cache and then
 * fallback to the other if the first attempt was unsuccessful.
 */
public class SplitCachesByImageSizeDiskCachePolicy implements DiskCachePolicy {

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final int mForceSmallCacheThresholdBytes;

  public SplitCachesByImageSizeDiskCachePolicy(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      int forceSmallCacheThresholdBytes) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mForceSmallCacheThresholdBytes = forceSmallCacheThresholdBytes;
  }

  @Override
  public Task<EncodedImage> createAndStartCacheReadTask(
      ImageRequest imageRequest,
      Object callerContext,
      final AtomicBoolean isCancelled) {
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, callerContext);
    final boolean alreadyInSmall = mSmallImageBufferedDiskCache.containsSync(cacheKey);
    final boolean alreadyInMain = mDefaultBufferedDiskCache.containsSync(cacheKey);
    final BufferedDiskCache firstCache;
    final BufferedDiskCache secondCache;
    if (alreadyInSmall || !alreadyInMain) {
      firstCache = mSmallImageBufferedDiskCache;
      secondCache = mDefaultBufferedDiskCache;
    } else {
      firstCache = mDefaultBufferedDiskCache;
      secondCache = mSmallImageBufferedDiskCache;
    }
    return firstCache.get(cacheKey, isCancelled)
        .continueWithTask(
            new Continuation<EncodedImage, Task<EncodedImage>>() {
              @Override
              public Task<EncodedImage> then(Task<EncodedImage> task) throws Exception {
                if (isTaskCancelled(task) || (!task.isFaulted() && task.getResult() != null)) {
                  return task;
                }
                return secondCache.get(cacheKey, isCancelled);
              }
            });
  }

  @Override
  public void writeToCache(
      EncodedImage newResult,
      ImageRequest imageRequest,
      Object callerContext) {
    final CacheKey cacheKey = mCacheKeyFactory.getEncodedCacheKey(imageRequest, callerContext);

    int size = newResult.getSize();
    if (size > 0 && size < mForceSmallCacheThresholdBytes) {
      mSmallImageBufferedDiskCache.put(cacheKey, newResult);
    } else {
      mDefaultBufferedDiskCache.put(cacheKey, newResult);
    }
  }

  private static boolean isTaskCancelled(Task<?> task) {
    return task.isCancelled() ||
        (task.isFaulted() && task.getError() instanceof CancellationException);
  }
}
