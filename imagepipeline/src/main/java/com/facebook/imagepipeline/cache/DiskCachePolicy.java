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

import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

import bolts.Task;

/**
 * Policy on how to read from and write to the image disk cache.
 *
 * <p> This is useful to separate from the producers how to decide which disk cache(s) to use,
 * whether the main or small cache, and also which precise cache key(s) to look for.
 */
public interface DiskCachePolicy {

  /**
   * Creates and starts the task to carry out a disk cache read, using whichever caches and keys are
   * appropriate for this policy.
   */
  Task<EncodedImage> createAndStartCacheReadTask(
      ImageRequest imageRequest,
      Object callerContext,
      AtomicBoolean isCancelled);

  /**
   * Writes the new image data to whichever cache and with whichever key is appropriate for this
   * policy.
   */
  void writeToCache(
      EncodedImage newResult,
      ImageRequest imageRequest,
      Object callerContext);
}
