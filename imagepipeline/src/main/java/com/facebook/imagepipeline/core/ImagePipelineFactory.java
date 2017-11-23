/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.FileCache;
import com.facebook.common.internal.AndroidPredicates;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.time.SystemClock;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.animated.factory.AnimatedFactory;
import com.facebook.imagepipeline.animated.factory.AnimatedFactoryProvider;
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
import com.facebook.imagepipeline.cache.MediaVariationsIndex;
import com.facebook.imagepipeline.cache.MediaVariationsIndexDatabase;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.cache.NoOpMediaVariationsIndex;
import com.facebook.imagepipeline.decoder.DefaultImageDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.platform.ArtDecoder;
import com.facebook.imagepipeline.platform.GingerbreadPurgeableDecoder;
import com.facebook.imagepipeline.platform.KitKatPurgeableDecoder;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import javax.annotation.concurrent.NotThreadSafe;

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
  private final ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  /**
   * Gets the instance of {@link ImagePipelineFactory}.
   */
  public static ImagePipelineFactory getInstance() {
    return Preconditions.checkNotNull(sInstance, "ImagePipelineFactory was not initialized!");
  }

  /**
   * Initializes {@link ImagePipelineFactory} with default config.
   */
  public static void initialize(Context context) {
    initialize(ImagePipelineConfig.newBuilder(context).build());
  }

  /**
   * Initializes {@link ImagePipelineFactory} with the specified config.
   */
  public static void initialize(ImagePipelineConfig imagePipelineConfig) {
    sInstance = new ImagePipelineFactory(imagePipelineConfig);
  }

  /**
   * Shuts {@link ImagePipelineFactory} down.
   */
  public static void shutDown() {
    if (sInstance != null) {
      sInstance.getBitmapMemoryCache().removeAll(AndroidPredicates.<CacheKey>True());
      sInstance.getEncodedMemoryCache().removeAll(AndroidPredicates.<CacheKey>True());
      sInstance = null;
    }
  }

  private final ImagePipelineConfig mConfig;
  private CountingMemoryCache<CacheKey, CloseableImage>
      mBitmapCountingMemoryCache;
  private MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
  private CountingMemoryCache<CacheKey, PooledByteBuffer> mEncodedCountingMemoryCache;
  private MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private BufferedDiskCache mMainBufferedDiskCache;
  private FileCache mMainFileCache;
  private ImageDecoder mImageDecoder;
  private ImagePipeline mImagePipeline;
  private ProducerFactory mProducerFactory;
  private ProducerSequenceFactory mProducerSequenceFactory;
  private BufferedDiskCache mSmallImageBufferedDiskCache;
  private FileCache mSmallImageFileCache;
  private MediaVariationsIndex mMediaVariationsIndex;

  private PlatformBitmapFactory mPlatformBitmapFactory;
  private PlatformDecoder mPlatformDecoder;

  private AnimatedFactory mAnimatedFactory;

  public ImagePipelineFactory(ImagePipelineConfig config) {
    mConfig = Preconditions.checkNotNull(config);
    mThreadHandoffProducerQueue = new ThreadHandoffProducerQueue(
        config.getExecutorSupplier().forLightweightBackgroundTasks());
  }

  @Nullable
  private AnimatedFactory getAnimatedFactory() {
    if (mAnimatedFactory == null) {
      mAnimatedFactory = AnimatedFactoryProvider.getAnimatedFactory(
          getPlatformBitmapFactory(),
          mConfig.getExecutorSupplier(),
          getBitmapCountingMemoryCache());
    }
    return mAnimatedFactory;
  }

  @Nullable
  public DrawableFactory getAnimatedDrawableFactory(Context context) {
    AnimatedFactory animatedFactory = getAnimatedFactory();
    return animatedFactory == null ? null : animatedFactory.getAnimatedDrawableFactory(context);
  }

  public CountingMemoryCache<CacheKey, CloseableImage>
  getBitmapCountingMemoryCache() {
    if (mBitmapCountingMemoryCache == null) {
      mBitmapCountingMemoryCache =
          BitmapCountingMemoryCacheFactory.get(
              mConfig.getBitmapMemoryCacheParamsSupplier(),
              mConfig.getMemoryTrimmableRegistry(),
              getPlatformBitmapFactory(),
              mConfig.getExperiments().isExternalCreatedBitmapLogEnabled(),
              mConfig.getBitmapMemoryCacheTrimStrategy());
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
              mConfig.getMemoryTrimmableRegistry(),
              getPlatformBitmapFactory());
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
        final AnimatedFactory animatedFactory = getAnimatedFactory();

        ImageDecoder gifDecoder = null;
        ImageDecoder webPDecoder = null;

        if (animatedFactory != null) {
          gifDecoder = animatedFactory.getGifDecoder(mConfig.getBitmapConfig());
          webPDecoder = animatedFactory.getWebPDecoder(mConfig.getBitmapConfig());
        }

        if (mConfig.getImageDecoderConfig() == null) {
          mImageDecoder = new DefaultImageDecoder(
              gifDecoder,
              webPDecoder,
              getPlatformDecoder());
        } else {
          mImageDecoder = new DefaultImageDecoder(
              gifDecoder,
              webPDecoder,
              getPlatformDecoder(),
              mConfig.getImageDecoderConfig().getCustomImageDecoders());
          // Add custom image formats if needed
          ImageFormatChecker.getInstance()
              .setCustomImageFormatCheckers(
                  mConfig.getImageDecoderConfig().getCustomImageFormats());
        }
      }
    }
    return mImageDecoder;
  }

  public BufferedDiskCache getMainBufferedDiskCache() {
    if (mMainBufferedDiskCache == null) {
      mMainBufferedDiskCache =
          new BufferedDiskCache(
              getMainFileCache(),
              mConfig.getPoolFactory().getPooledByteBufferFactory(),
              mConfig.getPoolFactory().getPooledByteStreams(),
              mConfig.getExecutorSupplier().forLocalStorageRead(),
              mConfig.getExecutorSupplier().forLocalStorageWrite(),
              mConfig.getImageCacheStatsTracker());
    }
    return mMainBufferedDiskCache;
  }

  public FileCache getMainFileCache() {
    if (mMainFileCache == null) {
      DiskCacheConfig diskCacheConfig = mConfig.getMainDiskCacheConfig();
      mMainFileCache = mConfig.getFileCacheFactory().get(diskCacheConfig);
    }
    return mMainFileCache;
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
              mConfig.getCacheKeyFactory(),
              mThreadHandoffProducerQueue,
              Suppliers.of(false));
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
  public static PlatformDecoder buildPlatformDecoder(
      PoolFactory poolFactory,
      boolean directWebpDirectDecodingEnabled) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      int maxNumThreads = poolFactory.getFlexByteArrayPoolMaxNumThreads();
      return new ArtDecoder(
          poolFactory.getBitmapPool(),
          maxNumThreads,
          new Pools.SynchronizedPool<>(maxNumThreads));
    } else {
      if (directWebpDirectDecodingEnabled
          && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        return new GingerbreadPurgeableDecoder();
      } else {
        return new KitKatPurgeableDecoder(poolFactory.getFlexByteArrayPool());
      }
    }
  }

  public PlatformDecoder getPlatformDecoder() {
    if (mPlatformDecoder == null) {
      mPlatformDecoder = buildPlatformDecoder(
          mConfig.getPoolFactory(),
          mConfig.getExperiments().isWebpSupportEnabled());
    }
    return mPlatformDecoder;
  }

  private ProducerFactory getProducerFactory() {
    if (mProducerFactory == null) {
      mProducerFactory =
          mConfig
              .getExperiments()
              .getProducerFactoryMethod()
              .createProducerFactory(
                  mConfig.getContext(),
                  mConfig.getPoolFactory().getSmallByteArrayPool(),
                  getImageDecoder(),
                  mConfig.getProgressiveJpegConfig(),
                  mConfig.isDownsampleEnabled(),
                  mConfig.isResizeAndRotateEnabledForNetwork(),
                  mConfig.getExperiments().isDecodeCancellationEnabled(),
                  mConfig.getExperiments().isSmartResizingEnabled(),
                  mConfig.getExecutorSupplier(),
                  mConfig.getPoolFactory().getPooledByteBufferFactory(),
                  getBitmapMemoryCache(),
                  getEncodedMemoryCache(),
                  getMainBufferedDiskCache(),
                  getSmallImageBufferedDiskCache(),
                  getMediaVariationsIndex(),
                  mConfig.getCacheKeyFactory(),
                  getPlatformBitmapFactory(),
                  mConfig.getExperiments().getBitmapPrepareToDrawMinSizeBytes(),
                  mConfig.getExperiments().getBitmapPrepareToDrawMaxSizeBytes(),
                  mConfig.getExperiments().getBitmapPrepareToDrawForPrefetch());
    }
    return mProducerFactory;
  }

  private ProducerSequenceFactory getProducerSequenceFactory() {
    // before Android N the Bitmap#prepareToDraw method is no-op so do not need this
    final boolean useBitmapPrepareToDraw = Build.VERSION.SDK_INT >= 24 //Build.VERSION_CODES.NOUGAT
        && mConfig.getExperiments().getUseBitmapPrepareToDraw();

    if (mProducerSequenceFactory == null) {
      mProducerSequenceFactory =
          new ProducerSequenceFactory(
              mConfig.getContext().getApplicationContext().getContentResolver(),
              getProducerFactory(),
              mConfig.getNetworkFetcher(),
              mConfig.isResizeAndRotateEnabledForNetwork(),
              mConfig.getExperiments().isWebpSupportEnabled(),
              mThreadHandoffProducerQueue,
              mConfig.getExperiments().getUseDownsamplingRatioForResizing(),
              useBitmapPrepareToDraw,
              mConfig.getExperiments().isPartialImageCachingEnabled());
    }
    return mProducerSequenceFactory;
  }

  public FileCache getSmallImageFileCache() {
    if (mSmallImageFileCache == null) {
      DiskCacheConfig diskCacheConfig = mConfig.getSmallImageDiskCacheConfig();
      mSmallImageFileCache = mConfig.getFileCacheFactory().get(diskCacheConfig);
    }
    return mSmallImageFileCache;
  }

  private BufferedDiskCache getSmallImageBufferedDiskCache() {
    if (mSmallImageBufferedDiskCache == null) {
      mSmallImageBufferedDiskCache =
          new BufferedDiskCache(
              getSmallImageFileCache(),
              mConfig.getPoolFactory().getPooledByteBufferFactory(),
              mConfig.getPoolFactory().getPooledByteStreams(),
              mConfig.getExecutorSupplier().forLocalStorageRead(),
              mConfig.getExecutorSupplier().forLocalStorageWrite(),
              mConfig.getImageCacheStatsTracker());
    }
    return mSmallImageBufferedDiskCache;
  }

  public MediaVariationsIndex getMediaVariationsIndex() {
    if (mMediaVariationsIndex == null) {
      mMediaVariationsIndex =
          mConfig.getExperiments().getMediaVariationsIndexEnabled()
              ? new MediaVariationsIndexDatabase(
                  mConfig.getContext(),
                  mConfig.getExecutorSupplier().forLocalStorageRead(),
                  mConfig.getExecutorSupplier().forLocalStorageWrite(),
                  SystemClock.get())
              : new NoOpMediaVariationsIndex();
    }

    return mMediaVariationsIndex;
  }
}
