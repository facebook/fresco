/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import android.content.Context;
import android.os.Build;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.cache.disk.FileCache;
import com.facebook.common.internal.AndroidPredicates;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.animated.factory.AnimatedFactory;
import com.facebook.imagepipeline.animated.factory.AnimatedFactoryProvider;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactoryProvider;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.EncodedCountingMemoryCacheFactory;
import com.facebook.imagepipeline.cache.EncodedMemoryCacheFactory;
import com.facebook.imagepipeline.cache.InstrumentedMemoryCache;
import com.facebook.imagepipeline.cache.InstrumentedMemoryCacheBitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.decoder.DefaultImageDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.imagepipeline.platform.PlatformDecoderFactory;
import com.facebook.imagepipeline.producers.ExperimentalThreadHandoffProducerQueueImpl;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueueImpl;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.imagepipeline.transcoder.ImageTranscoder;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import com.facebook.imagepipeline.transcoder.MultiImageTranscoderFactory;
import com.facebook.imagepipeline.transcoder.SimpleImageTranscoderFactory;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Factory class for the image pipeline.
 *
 * <p>This class constructs the pipeline and its dependencies from other libraries.
 *
 * <p>As the pipeline object can be quite expensive to create, it is strongly recommended that
 * applications create just one instance of this class and of the pipeline.
 */
@NotThreadSafe
public class ImagePipelineFactory {

  private static final Class<?> TAG = ImagePipelineFactory.class;

  private static ImagePipelineFactory sInstance = null;
  private static boolean sForceSinglePipelineInstance;
  private static ImagePipeline sImagePipeline;
  private final ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  /** Gets the instance of {@link ImagePipelineFactory}. */
  public static ImagePipelineFactory getInstance() {
    return Preconditions.checkNotNull(sInstance, "ImagePipelineFactory was not initialized!");
  }

  /**
   * Overrides current instance with a new one. Usually used when dealing with multiple
   * ImagePipelineFactories
   *
   * @param newInstance
   */
  public static void setInstance(ImagePipelineFactory newInstance) {
    sInstance = newInstance;
  }

  /** Initializes {@link ImagePipelineFactory} with default config. */
  public static synchronized void initialize(Context context) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipelineFactory#initialize");
    }
    initialize(ImagePipelineConfig.newBuilder(context).build());
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  /** Initializes {@link ImagePipelineFactory} with the specified config. */
  public static synchronized void initialize(
      ImagePipelineConfig imagePipelineConfig, boolean forceSinglePipelineInstance) {
    if (sInstance != null) {
      FLog.w(
          TAG,
          "ImagePipelineFactory has already been initialized! `ImagePipelineFactory.initialize(...)` should only be called once to avoid unexpected behavior.");
    }

    sForceSinglePipelineInstance = forceSinglePipelineInstance;
    sInstance = new ImagePipelineFactory(imagePipelineConfig);
  }

  /** Initializes {@link ImagePipelineFactory} with the specified config. */
  public static synchronized void initialize(ImagePipelineConfig imagePipelineConfig) {
    if (sInstance != null) {
      FLog.w(
          TAG,
          "ImagePipelineFactory has already been initialized! `ImagePipelineFactory.initialize(...)` should only be called once to avoid unexpected behavior.");
    }

    sInstance = new ImagePipelineFactory(imagePipelineConfig);
  }

  /** Checks if {@link ImagePipelineFactory} has already been initialized */
  public static synchronized boolean hasBeenInitialized() {
    return sInstance != null;
  }

  /** Shuts {@link ImagePipelineFactory} down. */
  public static synchronized void shutDown() {
    if (sInstance != null) {
      sInstance.getBitmapMemoryCache().removeAll(AndroidPredicates.<CacheKey>True());
      sInstance.getEncodedMemoryCache().removeAll(AndroidPredicates.<CacheKey>True());
      sInstance = null;
    }
  }

  private final ImagePipelineConfig mConfig;
  private final CloseableReferenceFactory mCloseableReferenceFactory;
  private CountingMemoryCache<CacheKey, CloseableImage> mBitmapCountingMemoryCache;
  private InstrumentedMemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
  private CountingMemoryCache<CacheKey, PooledByteBuffer> mEncodedCountingMemoryCache;
  private InstrumentedMemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private BufferedDiskCache mMainBufferedDiskCache;
  private FileCache mMainFileCache;
  private ImageDecoder mImageDecoder;
  private ImagePipeline mImagePipeline;
  private ImageTranscoderFactory mImageTranscoderFactory;
  private ProducerFactory mProducerFactory;
  private ProducerSequenceFactory mProducerSequenceFactory;
  private BufferedDiskCache mSmallImageBufferedDiskCache;
  private FileCache mSmallImageFileCache;

  private PlatformBitmapFactory mPlatformBitmapFactory;
  private PlatformDecoder mPlatformDecoder;

  private AnimatedFactory mAnimatedFactory;

  public ImagePipelineFactory(ImagePipelineConfig config) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipelineConfig()");
    }
    mConfig = Preconditions.checkNotNull(config);
    mThreadHandoffProducerQueue =
        mConfig.getExperiments().isExperimentalThreadHandoffQueueEnabled()
            ? new ExperimentalThreadHandoffProducerQueueImpl(
                config.getExecutorSupplier().forLightweightBackgroundTasks())
            : new ThreadHandoffProducerQueueImpl(
                config.getExecutorSupplier().forLightweightBackgroundTasks());
    CloseableReference.setDisableCloseableReferencesForBitmaps(
        config.getExperiments().getBitmapCloseableRefType());
    mCloseableReferenceFactory =
        new CloseableReferenceFactory(config.getCloseableReferenceLeakTracker());
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  @Nullable
  private AnimatedFactory getAnimatedFactory() {
    if (mAnimatedFactory == null) {
      mAnimatedFactory =
          AnimatedFactoryProvider.getAnimatedFactory(
              getPlatformBitmapFactory(),
              mConfig.getExecutorSupplier(),
              getBitmapCountingMemoryCache(),
              mConfig.getExperiments().shouldDownscaleFrameToDrawableDimensions());
    }
    return mAnimatedFactory;
  }

  @Nullable
  public DrawableFactory getAnimatedDrawableFactory(Context context) {
    AnimatedFactory animatedFactory = getAnimatedFactory();
    return animatedFactory == null ? null : animatedFactory.getAnimatedDrawableFactory(context);
  }

  public CountingMemoryCache<CacheKey, CloseableImage> getBitmapCountingMemoryCache() {
    if (mBitmapCountingMemoryCache == null) {
      mBitmapCountingMemoryCache =
          mConfig
              .getBitmapMemoryCacheFactory()
              .create(
                  mConfig.getBitmapMemoryCacheParamsSupplier(),
                  mConfig.getMemoryTrimmableRegistry(),
                  mConfig.getBitmapMemoryCacheTrimStrategy(),
                  mConfig.getBitmapMemoryCacheEntryStateObserver());
    }
    return mBitmapCountingMemoryCache;
  }

  public InstrumentedMemoryCache<CacheKey, CloseableImage> getBitmapMemoryCache() {
    if (mBitmapMemoryCache == null) {
      MemoryCache<CacheKey, CloseableImage> backingCache = getBitmapCountingMemoryCache();
      mBitmapMemoryCache =
          InstrumentedMemoryCacheBitmapMemoryCacheFactory.get(
              backingCache, mConfig.getImageCacheStatsTracker());
    }
    return mBitmapMemoryCache;
  }

  public CountingMemoryCache<CacheKey, PooledByteBuffer> getEncodedCountingMemoryCache() {
    if (mEncodedCountingMemoryCache == null) {
      mEncodedCountingMemoryCache =
          EncodedCountingMemoryCacheFactory.get(
              mConfig.getEncodedMemoryCacheParamsSupplier(), mConfig.getMemoryTrimmableRegistry());
    }
    return mEncodedCountingMemoryCache;
  }

  public InstrumentedMemoryCache<CacheKey, PooledByteBuffer> getEncodedMemoryCache() {
    if (mEncodedMemoryCache == null) {
      MemoryCache<CacheKey, PooledByteBuffer> backingCache =
          mConfig.getEncodedMemoryCacheOverride() != null
              ? mConfig.getEncodedMemoryCacheOverride()
              : getEncodedCountingMemoryCache();
      mEncodedMemoryCache =
          EncodedMemoryCacheFactory.get(backingCache, mConfig.getImageCacheStatsTracker());
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
          mImageDecoder = new DefaultImageDecoder(gifDecoder, webPDecoder, getPlatformDecoder());
        } else {
          mImageDecoder =
              new DefaultImageDecoder(
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
              mConfig.getPoolFactory().getPooledByteBufferFactory(mConfig.getMemoryChunkType()),
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
    if (sForceSinglePipelineInstance) {
      if (sImagePipeline == null) {
        sImagePipeline = createImagePipeline();
        mImagePipeline = sImagePipeline;
      }
      return sImagePipeline;
    }
    if (mImagePipeline == null) {
      mImagePipeline = createImagePipeline();
    }
    return mImagePipeline;
  }

  private ImagePipeline createImagePipeline() {
    return new ImagePipeline(
        getProducerSequenceFactory(),
        mConfig.getRequestListeners(),
        mConfig.getRequestListener2s(),
        mConfig.getIsPrefetchEnabledSupplier(),
        getBitmapMemoryCache(),
        getEncodedMemoryCache(),
        getMainBufferedDiskCache(),
        getSmallImageBufferedDiskCache(),
        mConfig.getCacheKeyFactory(),
        mThreadHandoffProducerQueue,
        mConfig.getExperiments().getSuppressBitmapPrefetchingSupplier(),
        mConfig.getExperiments().isLazyDataSource(),
        mConfig.getCallerContextVerifier(),
        mConfig);
  }

  public PlatformBitmapFactory getPlatformBitmapFactory() {
    if (mPlatformBitmapFactory == null) {
      mPlatformBitmapFactory =
          PlatformBitmapFactoryProvider.buildPlatformBitmapFactory(
              mConfig.getPoolFactory(), getPlatformDecoder(), getCloseableReferenceFactory());
    }
    return mPlatformBitmapFactory;
  }

  public PlatformDecoder getPlatformDecoder() {
    if (mPlatformDecoder == null) {
      mPlatformDecoder =
          PlatformDecoderFactory.buildPlatformDecoder(
              mConfig.getPoolFactory(), mConfig.getExperiments().isGingerbreadDecoderEnabled());
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
                  mConfig.getExecutorSupplier(),
                  mConfig.getPoolFactory().getPooledByteBufferFactory(mConfig.getMemoryChunkType()),
                  getBitmapMemoryCache(),
                  getEncodedMemoryCache(),
                  getMainBufferedDiskCache(),
                  getSmallImageBufferedDiskCache(),
                  mConfig.getCacheKeyFactory(),
                  getPlatformBitmapFactory(),
                  mConfig.getExperiments().getBitmapPrepareToDrawMinSizeBytes(),
                  mConfig.getExperiments().getBitmapPrepareToDrawMaxSizeBytes(),
                  mConfig.getExperiments().getBitmapPrepareToDrawForPrefetch(),
                  mConfig.getExperiments().getMaxBitmapSize(),
                  getCloseableReferenceFactory(),
                  mConfig.getExperiments().shouldKeepCancelledFetchAsLowPriority(),
                  mConfig.getExperiments().getTrackedKeysSize());
    }
    return mProducerFactory;
  }

  private ProducerSequenceFactory getProducerSequenceFactory() {
    // before Android N the Bitmap#prepareToDraw method is no-op so do not need this
    final boolean useBitmapPrepareToDraw =
        Build.VERSION.SDK_INT >= 24 // Build.VERSION_CODES.NOUGAT
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
              mConfig.isDownsampleEnabled(),
              useBitmapPrepareToDraw,
              mConfig.getExperiments().isPartialImageCachingEnabled(),
              mConfig.isDiskCacheEnabled(),
              getImageTranscoderFactory(),
              mConfig.getExperiments().isEncodedMemoryCacheProbingEnabled(),
              mConfig.getExperiments().isDiskCacheProbingEnabled());
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

  public CloseableReferenceFactory getCloseableReferenceFactory() {
    return mCloseableReferenceFactory;
  }

  private BufferedDiskCache getSmallImageBufferedDiskCache() {
    if (mSmallImageBufferedDiskCache == null) {
      mSmallImageBufferedDiskCache =
          new BufferedDiskCache(
              getSmallImageFileCache(),
              mConfig.getPoolFactory().getPooledByteBufferFactory(mConfig.getMemoryChunkType()),
              mConfig.getPoolFactory().getPooledByteStreams(),
              mConfig.getExecutorSupplier().forLocalStorageRead(),
              mConfig.getExecutorSupplier().forLocalStorageWrite(),
              mConfig.getImageCacheStatsTracker());
    }
    return mSmallImageBufferedDiskCache;
  }

  /**
   * Defines the correct {@link ImageTranscoder}. If a custom {@link ImageTranscoder} was define in
   * the config, it will be used whenever possible. Else, if the native code is disabled it uses
   * {@link SimpleImageTranscoderFactory}. Otherwise {@link MultiImageTranscoderFactory} determines
   * the {@link ImageTranscoder} to use based on the image format.
   *
   * @return The {@link ImageTranscoderFactory}
   */
  private ImageTranscoderFactory getImageTranscoderFactory() {
    if (mImageTranscoderFactory == null) {
      if (mConfig.getImageTranscoderFactory() == null
          && mConfig.getImageTranscoderType() == null
          && mConfig.getExperiments().isNativeCodeDisabled()) {
        mImageTranscoderFactory =
            new SimpleImageTranscoderFactory(mConfig.getExperiments().getMaxBitmapSize());
      } else {
        mImageTranscoderFactory =
            new MultiImageTranscoderFactory(
                mConfig.getExperiments().getMaxBitmapSize(),
                mConfig.getExperiments().getUseDownsamplingRatioForResizing(),
                mConfig.getImageTranscoderFactory(),
                mConfig.getImageTranscoderType(),
                mConfig.getExperiments().isEnsureTranscoderLibraryLoaded());
      }
    }
    return mImageTranscoderFactory;
  }

  @Nullable
  public String reportData() {
    return Objects.toStringHelper("ImagePipelineFactory")
        .add("bitmapCountingMemoryCache", mBitmapCountingMemoryCache.getDebugData())
        .add("encodedCountingMemoryCache", mEncodedCountingMemoryCache.getDebugData())
        .toString();
  }
}
