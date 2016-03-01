/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.request;

import javax.annotation.Nullable;

import android.graphics.Bitmap;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;

/**
 * Use an instance of this class to perform post-process operations on a bitmap.
 */
public interface Postprocessor {

  /**
   * Called by the pipeline after completing other steps.
   *
   * @param sourceBitmap The source bitmap.
   * @param bitmapFactory The factory to create a destination bitmap.
   *
   * <p> The Postprocessor must not modify the source bitmap as it may be shared by the other
   * clients. The implementation must create a new bitmap that is safe to be modified and return a
   * reference to it. To create a bitmap, use the provided <code>bitmapFactory</code>.
   */
  CloseableReference<Bitmap> process(Bitmap sourceBitmap, PlatformBitmapFactory bitmapFactory);

  /**
   * Returns the name of this postprocessor.
   *
   * <p>Used for logging and analytics.
   */
  String getName();

  /**
   * Implement this method in order to cache the result of a postprocessor in the bitmap cache
   * along with the unmodified image.
   * <p>When reading from memory cache, there will be a hit only if the cache's value for this key
   * matches that of the request.
   * <p>Each postprocessor class is only allowed one entry in the cache. When <i>writing</i> to
   * memory cache, this key is not considered and any image for this request with the same
   * postprocessor class will be overwritten.
   * @return The CacheKey to use for the result of this postprocessor
   */
  @Nullable
  CacheKey getPostprocessorCacheKey();
}
