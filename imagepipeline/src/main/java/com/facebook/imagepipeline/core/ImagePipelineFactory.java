/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import javax.annotation.concurrent.NotThreadSafe;

import java.util.concurrent.ScheduledExecutorService;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;

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
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactory;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImpl;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableCachingBackendImplProvider;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import com.facebook.imagepipeline.bitmaps.ArtBitmapFactory;
import com.facebook.imagepipeline.bitmaps.EmptyJpegGenerator;
import com.facebook.imagepipeline.bitmaps.GingerbreadBitmapFactory;
import com.facebook.imagepipeline.bitmaps.HoneycombBitmapFactory;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BitmapCountingMemoryCacheFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.EncodedCountingMemoryCacheFactory;
import com.facebook.imagepipeline.cache.EncodedMemoryCacheFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.platform.ArtDecoder;
import com.facebook.imagepipeline.platform.GingerbreadPurgeableDecoder;
import com.facebook.imagepipeline.platform.KitKatPurgeableDecoder;
import com.facebook.imagepipeline.platform.PlatformDecoder;

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

  private AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private AnimatedDrawableUtil mAnimatedDrawableUtil;
  private AnimatedDrawableFactory mAnimatedDrawableFactory;
  private AnimatedImageFactory mAnimatedImageFactory;
  private CountingMemoryCache<CacheKey, CloseableImage>
      mBitmapCountingMemoryCache;
  private MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
  private CountingMemoryCache<CacheKey, PooledByteBuffer> mEncodedCountingMemoryCache;
  private MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private BufferedDiskCache mMainBufferedDiskCache;
  private DiskStorageCache mMainDiskStorageCache;
  private ImageDecoder mImageDecoder;
  private ImagePipeline mImagePipeline;
  private ProducerFactory mProducerFactory;
  private ProducerSequenceFactory mProducerSequenceFactory;
  private BufferedDiskCache mSmallImageBufferedDiskCache;
  private DiskStorageCache mSmallImageDiskStorageCache;

  private PlatformBitmapFactory mPlatformBitmapFactory;
  private PlatformDecoder mPlatformDecoder;

  public ImagePipelineFactory(ImagePipelineConfig config) {
    mConfig = Preconditions.checkNotNull(config);
  }

  public static AnimatedDrawableFactory buildAnimatedDrawableFactory(
      final SerialExecutorService serialExecutorService,
      final ActivityManager activityManager,
      final AnimatedDrawableUtil animatedDrawableUtil,
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      ScheduledExecutorService scheduledExecutorService,
      final MonotonicClock monotonicClock,
      Resources resources) {
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


    return new AnimatedDrawableFactory(
        animatedDrawableBackendProvider,
        animatedDrawableCachingBackendImplProvider,
        animatedDrawableUtil,
        scheduledExecutorService,
        resources);
  }

  public AnimatedDrawableBackendProvider getAnimatedDrawableBackendProvider() {
    if (mAnimatedDrawableBackendProvider == null) {
      mAnimatedDrawableBackendProvider = new AnimatedDrawableBackendProvider() {
        @Override
        public AnimatedDrawableBackend get(AnimatedImageResult animatedImageResult, Rect bounds) {
          return new AnimatedDrawableBackendImpl(
              getAnimatedDrawableUtil(),
              animatedImageResult,
              bounds);
        }
      };
    }
    return mAnimatedDrawableBackendProvider;
  }

  public AnimatedDrawableFactory getAnimatedDrawableFactory() {
    if (mAnimatedDrawableFactory == null) {
      SerialExecutorService serialExecutorService =
          new DefaultSerialExecutorService(mConfig.getExecutorSupplier().forDecode());
      ActivityManager activityManager =
          (ActivityManager) mConfig.getContext().getSystemService(Context.ACTIVITY_SERVICE);
      mAnimatedDrawableFactory = buildAnimatedDrawableFactory(
          serialExecutorService,
          activityManager,
          getAnimatedDrawableUtil(),
          getAnimatedDrawableBackendProvider(),
          UiThreadImmediateExecutorService.getInstance(),
          RealtimeSinceBootClock.get(),
          mConfig.getContext().getResources());
    }
    return mAnimatedDrawableFactory;
  }

  // We need some of these methods public for now so internal code can use them.

  private AnimatedDrawableUtil getAnimatedDrawableUtil() {
    if (mAnimatedDrawableUtil == null) {
      mAnimatedDrawableUtil = new AnimatedDrawableUtil();
    }
    return mAnimatedDrawableUtil;
  }

  public static AnimatedImageFactory buildAnimatedImageFactory(
      final AnimatedDrawableUtil animatedDrawableUtil,
      PlatformBitmapFactory platformBitmapFactory) {
    AnimatedDrawableBackendProvider animatedDrawableBackendProvider =
        new AnimatedDrawableBackendProvider() {
          @Override
          public AnimatedDrawableBackend get(AnimatedImageResult imageResult, Rect bounds) {
            return new AnimatedDrawableBackendImpl(animatedDrawableUtil, imageResult, bounds);
          }
        };
   return new AnimatedImageFactory(animatedDrawableBackendProvider, platformBitmapFactory);
  }

  private AnimatedImageFactory getAnimatedImageFactory() {
    if (mAnimatedImageFactory == null) {
      if (mConfig.getAnimatedImageFactory() != null) {
        mAnimatedImageFactory = mConfig.getAnimatedImageFactory();
      } else {
        mAnimatedImageFactory = buildAnimatedImageFactory(
            getAnimatedDrawableUtil(),
            getPlatformBitmapFactory());
      }
    }
    return mAnimatedImageFactory;
  }

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

  private ImageDecoder getImageDecoder() {
    if (mImageDecoder == null) {
      if (mConfig.getImageDecoder() != null) {
        mImageDecoder = mConfig.getImageDecoder();
      } else {
        mImageDecoder = new ImageDecoder(
            getAnimatedImageFactory(),
            getPlatformDecoder(),
            mConfig.getBitmapConfig());
      }
    }
    return mImageDecoder;
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
              getMainBufferedDiskCache(),
              getSmallImageBufferedDiskCache(),
              mConfig.getCacheKeyFactory());
    }
    return mImagePipeline;
  }

  /**
   * Provide the implementation of the PlatformBitmapFactory for the current platform
   * using the provided PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @param platformDecoder The PlatformDecoder
   * @return The PlatformBitmapFactory implementation
   */
  public static PlatformBitmapFactory buildPlatformBitmapFactory(
      PoolFactory poolFactory,
      PlatformDecoder platformDecoder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new ArtBitmapFactory(poolFactory.getBitmapPool());
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      return new HoneycombBitmapFactory(
          new EmptyJpegGenerator(poolFactory.getPooledByteBufferFactory()),
          platformDecoder);
    } else {
      return new GingerbreadBitmapFactory();
    }
  }

  public PlatformBitmapFactory getPlatformBitmapFactory() {
    if (mPlatformBitmapFactory == null) {
      mPlatformBitmapFactory = buildPlatformBitmapFactory(
          mConfig.getPoolFactory(),
          getPlatformDecoder());
    }
    return mPlatformBitmapFactory;
  }

  /**
   * Provide the implementation of the PlatformDecoder for the current platform using the
   * provided PoolFactory
   *
   * @param poolFactory The PoolFactory
   * @return The PlatformDecoder implementation
   */
  public static PlatformDecoder buildPlatformDecoder(PoolFactory poolFactory) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new ArtDecoder(
          poolFactory.getBitmapPool(),
          poolFactory.getFlexByteArrayPoolMaxNumThreads());
    } else {
      // Fix for purgeable failure in GingerbreadPurgeableDecoder
      return new KitKatPurgeableDecoder(poolFactory.getFlexByteArrayPool());
    }
  }

  public PlatformDecoder getPlatformDecoder() {
    if (mPlatformDecoder == null) {
      mPlatformDecoder = buildPlatformDecoder(mConfig.getPoolFactory());
    }
    return mPlatformDecoder;
  }

  private ProducerFactory getProducerFactory() {
    if (mProducerFactory == null) {
      mProducerFactory =
          new ProducerFactory(
              mConfig.getContext(),
              mConfig.getPoolFactory().getSmallByteArrayPool(),
              getImageDecoder(),
              mConfig.getProgressiveJpegConfig(),
              mConfig.isDownsampleEnabled(),
              mConfig.isResizeAndRotateEnabledForNetwork(),
              mConfig.getExecutorSupplier(),
              mConfig.getPoolFactory().getPooledByteBufferFactory(),
              getBitmapMemoryCache(),
              getEncodedMemoryCache(),
              getMainBufferedDiskCache(),
              getSmallImageBufferedDiskCache(),
              mConfig.getCacheKeyFactory(),
              getPlatformBitmapFactory(),
              mConfig.isDecodeFileDescriptorEnabled());
    }
    return mProducerFactory;
  }

  private ProducerSequenceFactory getProducerSequenceFactory() {
    if (mProducerSequenceFactory == null) {
      mProducerSequenceFactory =
          new ProducerSequenceFactory(
              getProducerFactory(),
              mConfig.getNetworkFetcher(),
              mConfig.isResizeAndRotateEnabledForNetwork(),
              mConfig.isDownsampleEnabled());
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
}
