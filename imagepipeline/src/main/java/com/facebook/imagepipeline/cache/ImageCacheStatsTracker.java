/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

/**
 * Interface for stats tracking for the image cache.
 *
 * <p>An implementation of this interface, passed to
 * {@link com.facebook.imagepipeline.core.ImagePipelineConfig}, will be notified for each
 * of the following cache events. Use this to keep cache stats for your app.
 */
public interface ImageCacheStatsTracker {

  /** Called whenever decoded images are put into the bitmap cache. */
  public void onBitmapCachePut();

  /** Called on a bitmap cache hit. */
  public void onBitmapCacheHit();

  /** Called on a bitmap cache miss. */
  public void onBitmapCacheMiss();

  /** Called whenever encoded images are put into the encoded memory cache. */
  public void onMemoryCachePut();

  /** Called on an encoded memory cache hit. */
  public void onMemoryCacheHit();

  /** Called on an encoded memory cache hit. */
  public void onMemoryCacheMiss();

  /**
   * Called on an staging area hit.
   *
   * <p>The staging area stores encoded images. It gets the images before they are written
   * to disk cache.
   */
  public void onStagingAreaHit();

  /** Called on a staging area miss hit. */
  public void onStagingAreaMiss();

  /** Called on a disk cache hit. */
  public void onDiskCacheHit();

  /** Called on a disk cache miss. */
  public void onDiskCacheMiss();

  /** Called if an exception is thrown on a disk cache read. */
  public void onDiskCacheGetFail();

  /**
   * Registers a bitmap cache with this tracker.
   *
   * <p>Use this method if you need access to the cache itself to compile your stats.
   */
  public void registerBitmapMemoryCache(CountingMemoryCache<?, ?> bitmapMemoryCache);

  /**
   * Registers an encoded memory cache with this tracker.
   *
   * <p>Use this method if you need access to the cache itself to compile your stats.
   */
  public void registerEncodedMemoryCache(CountingMemoryCache<?, ?> encodedMemoryCache);
}
