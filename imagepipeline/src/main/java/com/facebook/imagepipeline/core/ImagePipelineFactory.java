/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;

import javax.annotation.concurrent.NotThreadSafe;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskCacheFactory;
import com.facebook.cache.disk.DiskStorageCache;
import com.facebook.common.executors.DefaultSerialExecutorService;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.AndroidPredicates;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.time.MonotonicClock;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableOptions;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.factory.AnimatedDrawableFactory;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImplProvider;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BitmapCountingMemoryCacheFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.EncodedCountingMemoryCacheFactory;
import com.facebook.imagepipeline.cache.EncodedMemoryCacheFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.bitmaps.EmptyJpegGenerator;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

/**
 * Factory class for the image pipeline.
 *
 * <p>This class constructs the pipeline and its dependencies from other libraries.
 *
 * <p>As the pipeline object can be quite expensive to create, it is strongly
 * recommended that applications create just one instance of this class
 * and of the pipeline.
 */
@NotThreadSafe
public class ImagePipelineFactory {

  private static ImagePipelineFactory sInstance = null;

  /** Gets the instance of {@link ImagePipelineFactory}. */
  public static ImagePipelineFactory getInstance() {
    return Preconditions.checkNotNull(sInstance, "ImagePipelineFactory was not initialized!");
  }

  /** Initializes {@link ImagePipelineFactory} with default config. */
  public static void initialize(Context context) {
    initialize(ImagePipelineConfig.newBuilder(context).build());
  }

  /** Initializes {@link ImagePipelineFactory} with the specified config. */
  public static void initialize(ImagePipelineConfig imagePipelineConfig) {
    sInstance = new ImagePipelineFactory(imagePipelineConfig);
  }

  /** Shuts {@link ImagePipelineFactory} down. */
  public static void shutDown() {
    if (sInstance != null) {
      sInstance.getBitmapMemoryCache().removeAll(AndroidPredicates.<CacheKey>True());
      sInstance.getEncodedMemoryCache().removeAll(AndroidPredicates.<CacheKey>True());
      sInstance = null;
    }
  }

  private final ImagePipelineConfig mConfig;

  private AnimatedDrawableFactory mAnimatedDrawableFactory;
  private CountingMemoryCache<CacheKey, CloseableImage>
      mBitmapCountingMemoryCache;
  private MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
  private EmptyJpegGenerator mEmptyJpegGenerator;
  private CountingMemoryCache<CacheKey, PooledByteBuffer> mEncodedCountingMemoryCache;
  private MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private BufferedDiskCache mMainBufferedDiskCache;
  private DiskStorageCache mMainDiskStorageCache;
  private ImagePipeline mImagePipeline;
  private ProducerFactory mProducerFactory;
  private ProducerSequenceFactory mProducerSequenceFactory;
  private BufferedDiskCache mSmallImageBufferedDiskCache;
  private DiskStorageCache mSmallImageDiskStorageCache;

  public ImagePipelineFactory(ImagePipelineConfig config) {
    mConfig = Preconditions.checkNotNull(config);
  }

  // We need some of these methods public for now so internal code can use them.

  public CountingMemoryCache<CacheKey, CloseableImage>
      getBitmapCountingMemoryCache() {
    if (mBitmapCountingMemoryCache == null) {
      mBitmapCountingMemoryCache =
          BitmapCountingMemoryCacheFactory.get(
              mConfig.getBitmapMemoryCacheParamsSupplier(),
              mConfig.getMemoryTrimmableRegistry());
    }
    return mBitmapCountingMemoryCache;
  }

  public MemoryCache<CacheKey, CloseableImage> getBitmapMemoryCache() {
    if (mBitmapMemoryCache == null) {
      mBitmapMemoryCache =
          BitmapMemoryCacheFactory.get(
              getBitmapCountingMemoryCache(),
              mConfig.getImageCacheStatsTracker());
    }
    return mBitmapMemoryCache;
  }

  public CountingMemoryCache<CacheKey, PooledByteBuffer> getEncodedCountingMemoryCache() {
    if (mEncodedCountingMemoryCache == null) {
      mEncodedCountingMemoryCache =
          EncodedCountingMemoryCacheFactory.get(
              mConfig.getEncodedMemoryCacheParamsSupplier(),
              mConfig.getMemoryTrimmableRegistry());
    }
    return mEncodedCountingMemoryCache;
  }

  public MemoryCache<CacheKey, PooledByteBuffer> getEncodedMemoryCache() {
    if (mEncodedMemoryCache == null) {
      mEncodedMemoryCache =
          EncodedMemoryCacheFactory.get(
              getEncodedCountingMemoryCache(),
              mConfig.getImageCacheStatsTracker());
    }
    return mEncodedMemoryCache;
  }

  private BufferedDiskCache getMainBufferedDiskCache() {
    if (mMainBufferedDiskCache == null) {
      mMainBufferedDiskCache =
          new BufferedDiskCache(
              getMainDiskStorageCache(),
              mConfig.getPoolFactory().getPooledByteBufferFactory(),
              mConfig.getPoolFactory().getPooledByteStreams(),
              mConfig.getExecutorSupplier().forLocalStorageRead(),
              mConfig.getExecutorSupplier().forLocalStorageWrite(),
              mConfig.getImageCacheStatsTracker());
    }
    return mMainBufferedDiskCache;
  }

  public DiskStorageCache getMainDiskStorageCache() {
    if (mMainDiskStorageCache == null) {
      mMainDiskStorageCache =
          DiskCacheFactory.newDiskStorageCache(mConfig.getMainDiskCacheConfig());
    }
    return mMainDiskStorageCache;
  }

  public ImagePipeline getImagePipeline() {
    if (mImagePipeline == null) {
      mImagePipeline =
          new ImagePipeline(
              getProducerSequenceFactory(),
              mConfig.getRequestListeners(),
              mConfig.getIsPrefetchEnabledSupplier(),
              getBitmapMemoryCache(),
              getEncodedMemoryCache(),
              mConfig.getCacheKeyFactory());
    }
    return mImagePipeline;
  }

  private ProducerFactory getProducerFactory() {
    if (mProducerFactory == null) {
      mProducerFactory =
          new ProducerFactory(
              mConfig.getContext(),
              mConfig.getPoolFactory().getCommonByteArrayPool(),
              mConfig.getImageDecoder(),
              mConfig.getProgressiveJpegConfig(),
              mConfig.getExecutorSupplier(),
              mConfig.getPoolFactory().getPooledByteBufferFactory(),
              getBitmapMemoryCache(),
              getEncodedMemoryCache(),
              getMainBufferedDiskCache(),
              getSmallImageBufferedDiskCache(),
              mConfig.getCacheKeyFactory(),
              mConfig.getPlatformBitmapFactory());
    }
    return mProducerFactory;
  }

  private ProducerSequenceFactory getProducerSequenceFactory() {
    if (mProducerSequenceFactory == null) {
      mProducerSequenceFactory =
          new ProducerSequenceFactory(
              getProducerFactory(),
              mConfig.getNetworkFetcher(),
              mConfig.isResizeAndRotateEnabledForNetwork());
    }
    return mProducerSequenceFactory;
  }

  public DiskStorageCache getSmallImageDiskStorageCache() {
    if (mSmallImageDiskStorageCache == null) {
      mSmallImageDiskStorageCache =
          DiskCacheFactory.newDiskStorageCache(mConfig.getSmallImageDiskCacheConfig());
    }
    return mSmallImageDiskStorageCache;
  }

  private BufferedDiskCache getSmallImageBufferedDiskCache() {
    if (mSmallImageBufferedDiskCache == null) {
      mSmallImageBufferedDiskCache =
          new BufferedDiskCache(
              getSmallImageDiskStorageCache(),
              mConfig.getPoolFactory().getPooledByteBufferFactory(),
              mConfig.getPoolFactory().getPooledByteStreams(),
              mConfig.getExecutorSupplier().forLocalStorageRead(),
              mConfig.getExecutorSupplier().forLocalStorageWrite(),
              mConfig.getImageCacheStatsTracker());
    }
    return mSmallImageBufferedDiskCache;
  }

  public AnimatedDrawableFactory getAnimatedDrawableFactory() {
    if (mAnimatedDrawableFactory == null) {
      final AnimatedDrawableUtil animatedDrawableUtil = new AnimatedDrawableUtil();
      final MonotonicClock monotonicClock = RealtimeSinceBootClock.get();
      final SerialExecutorService serialExecutorService =
          new DefaultSerialExecutorService(mConfig.getExecutorSupplier().forDecode());
      final ActivityManager activityManager =
          (ActivityManager) mConfig.getContext().getSystemService(Context.ACTIVITY_SERVICE);

      AnimatedDrawableCachingBackendImplProvider animatedDrawableCachingBackendImplProvider =
          new AnimatedDrawableCachingBackendImplProvider() {
            @Override
            public AnimatedDrawableCachingBackendImpl get(
                AnimatedDrawableBackend animatedDrawableBackend,
                AnimatedDrawableOptions options) {
              return new AnimatedDrawableCachingBackendImpl(
                  serialExecutorService,
                  activityManager,
                  animatedDrawableUtil,
                  monotonicClock,
                  animatedDrawableBackend,
                  options);
            }
          };

      AnimatedDrawableBackendProvider backendProvider = new AnimatedDrawableBackendProvider() {
        @Override
        public AnimatedDrawableBackend get(AnimatedImageResult animatedImageResult, Rect bounds) {
          return new AnimatedDrawableBackendImpl(animatedDrawableUtil, animatedImageResult, bounds);
        }
      };

      mAnimatedDrawableFactory = new AnimatedDrawableFactory(
          backendProvider,
          animatedDrawableCachingBackendImplProvider,
          animatedDrawableUtil,
          UiThreadImmediateExecutorService.getInstance(),
          mConfig.getContext().getResources());
    }
    return mAnimatedDrawableFactory;
  }
}
