/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.webp.BitmapCreator;
import com.facebook.common.webp.WebpBitmapFactory;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.imagepipeline.bitmaps.HoneycombBitmapCreator;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheTrimStrategy;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.CountingLruBitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.DefaultBitmapMemoryCacheParamsSupplier;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.cache.DefaultEncodedMemoryCacheParamsSupplier;
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.cache.NoOpImageCacheStatsTracker;
import com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker;
import com.facebook.imagepipeline.debug.NoOpCloseableReferenceLeakTracker;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoderConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.producers.HttpUrlConnectionNetworkFetcher;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Master configuration class for the image pipeline library.
 *
 * <p>To use: <code>
 *   ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
 *       .setXXX(xxx)
 *       .setYYY(yyy)
 *       .build();
 *   ImagePipelineFactory factory = new ImagePipelineFactory(config);
 *   ImagePipeline pipeline = factory.getImagePipeline();
 * </code>
 *
 * <p>This should only be done once per process.
 */
public class ImagePipelineConfig {
  // If a member here is marked @Nullable, it must be constructed by ImagePipelineFactory
  // on demand if needed.

  // There are a lot of parameters in this class. Please follow strict alphabetical order.
  private final Bitmap.Config mBitmapConfig;
  private final Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;
  private final MemoryCache.CacheTrimStrategy mBitmapMemoryCacheTrimStrategy;
  private final CountingMemoryCache.EntryStateObserver<CacheKey>
      mBitmapMemoryCacheEntryStateObserver;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Context mContext;
  private final boolean mDownsampleEnabled;
  private final FileCacheFactory mFileCacheFactory;
  private final Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;
  private final ExecutorSupplier mExecutorSupplier;
  private final ImageCacheStatsTracker mImageCacheStatsTracker;
  @Nullable private final ImageDecoder mImageDecoder;
  @Nullable private final ImageTranscoderFactory mImageTranscoderFactory;
  @Nullable @ImageTranscoderType private final Integer mImageTranscoderType;
  private final Supplier<Boolean> mIsPrefetchEnabledSupplier;
  private final DiskCacheConfig mMainDiskCacheConfig;
  private final MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  @MemoryChunkType private final int mMemoryChunkType;
  private final NetworkFetcher mNetworkFetcher;
  private final int mHttpNetworkTimeout;
  @Nullable private final PlatformBitmapFactory mPlatformBitmapFactory;
  private final PoolFactory mPoolFactory;
  private final ProgressiveJpegConfig mProgressiveJpegConfig;
  private final Set<RequestListener> mRequestListeners;
  private final Set<RequestListener2> mRequestListener2s;
  private final boolean mResizeAndRotateEnabledForNetwork;
  private final DiskCacheConfig mSmallImageDiskCacheConfig;
  @Nullable private final ImageDecoderConfig mImageDecoderConfig;
  private final ImagePipelineExperiments mImagePipelineExperiments;
  private final boolean mDiskCacheEnabled;
  @Nullable private final CallerContextVerifier mCallerContextVerifier;
  private final CloseableReferenceLeakTracker mCloseableReferenceLeakTracker;
  @Nullable private final MemoryCache<CacheKey, CloseableImage> mBitmapCache;
  @Nullable private final MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private final BitmapMemoryCacheFactory mBitmapMemoryCacheFactory;

  private static DefaultImageRequestConfig sDefaultImageRequestConfig =
      new DefaultImageRequestConfig();

  private ImagePipelineConfig(Builder builder) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipelineConfig()");
    }
    // We have to build experiments before the rest
    mImagePipelineExperiments = builder.mExperimentsBuilder.build();
    mBitmapMemoryCacheParamsSupplier =
        builder.mBitmapMemoryCacheParamsSupplier == null
            ? new DefaultBitmapMemoryCacheParamsSupplier(
                (ActivityManager) builder.mContext.getSystemService(Context.ACTIVITY_SERVICE))
            : builder.mBitmapMemoryCacheParamsSupplier;
    mBitmapMemoryCacheTrimStrategy =
        builder.mBitmapMemoryCacheTrimStrategy == null
            ? new BitmapMemoryCacheTrimStrategy()
            : builder.mBitmapMemoryCacheTrimStrategy;
    mBitmapMemoryCacheEntryStateObserver = builder.mBitmapMemoryCacheEntryStateObserver;
    mBitmapConfig = builder.mBitmapConfig == null ? Bitmap.Config.ARGB_8888 : builder.mBitmapConfig;
    mCacheKeyFactory =
        builder.mCacheKeyFactory == null
            ? DefaultCacheKeyFactory.getInstance()
            : builder.mCacheKeyFactory;
    mContext = Preconditions.checkNotNull(builder.mContext);
    mFileCacheFactory =
        builder.mFileCacheFactory == null
            ? new DiskStorageCacheFactory(new DynamicDefaultDiskStorageFactory())
            : builder.mFileCacheFactory;
    mDownsampleEnabled = builder.mDownsampleEnabled;
    mEncodedMemoryCacheParamsSupplier =
        builder.mEncodedMemoryCacheParamsSupplier == null
            ? new DefaultEncodedMemoryCacheParamsSupplier()
            : builder.mEncodedMemoryCacheParamsSupplier;
    mImageCacheStatsTracker =
        builder.mImageCacheStatsTracker == null
            ? NoOpImageCacheStatsTracker.getInstance()
            : builder.mImageCacheStatsTracker;
    mImageDecoder = builder.mImageDecoder;
    mImageTranscoderFactory = getImageTranscoderFactory(builder);
    mImageTranscoderType = builder.mImageTranscoderType;
    mIsPrefetchEnabledSupplier =
        builder.mIsPrefetchEnabledSupplier == null
            ? new Supplier<Boolean>() {
              @Override
              public Boolean get() {
                return true;
              }
            }
            : builder.mIsPrefetchEnabledSupplier;
    mMainDiskCacheConfig =
        builder.mMainDiskCacheConfig == null
            ? getDefaultMainDiskCacheConfig(builder.mContext)
            : builder.mMainDiskCacheConfig;
    mMemoryTrimmableRegistry =
        builder.mMemoryTrimmableRegistry == null
            ? NoOpMemoryTrimmableRegistry.getInstance()
            : builder.mMemoryTrimmableRegistry;
    mMemoryChunkType = getMemoryChunkType(builder, mImagePipelineExperiments);
    mHttpNetworkTimeout =
        builder.mHttpConnectionTimeout < 0
            ? HttpUrlConnectionNetworkFetcher.HTTP_DEFAULT_TIMEOUT
            : builder.mHttpConnectionTimeout;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ImagePipelineConfig->mNetworkFetcher");
    }
    mNetworkFetcher =
        builder.mNetworkFetcher == null
            ? new HttpUrlConnectionNetworkFetcher(mHttpNetworkTimeout)
            : builder.mNetworkFetcher;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    mPlatformBitmapFactory = builder.mPlatformBitmapFactory;
    mPoolFactory =
        builder.mPoolFactory == null
            ? new PoolFactory(PoolConfig.newBuilder().build())
            : builder.mPoolFactory;
    mProgressiveJpegConfig =
        builder.mProgressiveJpegConfig == null
            ? new SimpleProgressiveJpegConfig()
            : builder.mProgressiveJpegConfig;
    mRequestListeners =
        builder.mRequestListeners == null
            ? new HashSet<RequestListener>()
            : builder.mRequestListeners;
    mRequestListener2s =
        builder.mRequestListener2s == null
            ? new HashSet<RequestListener2>()
            : builder.mRequestListener2s;
    mResizeAndRotateEnabledForNetwork = builder.mResizeAndRotateEnabledForNetwork;
    mSmallImageDiskCacheConfig =
        builder.mSmallImageDiskCacheConfig == null
            ? mMainDiskCacheConfig
            : builder.mSmallImageDiskCacheConfig;
    mImageDecoderConfig = builder.mImageDecoderConfig;
    // Below this comment can't be built in alphabetical order, because of dependencies
    int numCpuBoundThreads = mPoolFactory.getFlexByteArrayPoolMaxNumThreads();
    mExecutorSupplier =
        builder.mExecutorSupplier == null
            ? new DefaultExecutorSupplier(numCpuBoundThreads)
            : builder.mExecutorSupplier;
    mDiskCacheEnabled = builder.mDiskCacheEnabled;
    mCallerContextVerifier = builder.mCallerContextVerifier;
    mCloseableReferenceLeakTracker = builder.mCloseableReferenceLeakTracker;
    mBitmapCache = builder.mBitmapMemoryCache;
    mBitmapMemoryCacheFactory =
        builder.mBitmapMemoryCacheFactory == null
            ? new CountingLruBitmapMemoryCacheFactory()
            : builder.mBitmapMemoryCacheFactory;
    mEncodedMemoryCache = builder.mEncodedMemoryCache;
    // Here we manage the WebpBitmapFactory implementation if any
    WebpBitmapFactory webpBitmapFactory = mImagePipelineExperiments.getWebpBitmapFactory();
    if (webpBitmapFactory != null) {
      BitmapCreator bitmapCreator = new HoneycombBitmapCreator(getPoolFactory());
      setWebpBitmapFactory(webpBitmapFactory, mImagePipelineExperiments, bitmapCreator);
    } else {
      // We check using introspection only if the experiment is enabled
      if (mImagePipelineExperiments.isWebpSupportEnabled()
          && WebpSupportStatus.sIsWebpSupportRequired) {
        webpBitmapFactory = WebpSupportStatus.loadWebpBitmapFactoryIfExists();
        if (webpBitmapFactory != null) {
          BitmapCreator bitmapCreator = new HoneycombBitmapCreator(getPoolFactory());
          setWebpBitmapFactory(webpBitmapFactory, mImagePipelineExperiments, bitmapCreator);
        }
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  private static void setWebpBitmapFactory(
      final WebpBitmapFactory webpBitmapFactory,
      final ImagePipelineExperiments imagePipelineExperiments,
      final BitmapCreator bitmapCreator) {
    WebpSupportStatus.sWebpBitmapFactory = webpBitmapFactory;
    final WebpBitmapFactory.WebpErrorLogger webpErrorLogger =
        imagePipelineExperiments.getWebpErrorLogger();
    if (webpErrorLogger != null) {
      webpBitmapFactory.setWebpErrorLogger(webpErrorLogger);
    }
    if (bitmapCreator != null) {
      webpBitmapFactory.setBitmapCreator(bitmapCreator);
    }
  }

  private static DiskCacheConfig getDefaultMainDiskCacheConfig(final Context context) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("DiskCacheConfig.getDefaultMainDiskCacheConfig");
      }
      return DiskCacheConfig.newBuilder(context).build();
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @VisibleForTesting
  static void resetDefaultRequestConfig() {
    sDefaultImageRequestConfig = new DefaultImageRequestConfig();
  }

  public Bitmap.Config getBitmapConfig() {
    return mBitmapConfig;
  }

  public Supplier<MemoryCacheParams> getBitmapMemoryCacheParamsSupplier() {
    return mBitmapMemoryCacheParamsSupplier;
  }

  public MemoryCache.CacheTrimStrategy getBitmapMemoryCacheTrimStrategy() {
    return mBitmapMemoryCacheTrimStrategy;
  }

  public CountingMemoryCache.EntryStateObserver<CacheKey> getBitmapMemoryCacheEntryStateObserver() {
    return mBitmapMemoryCacheEntryStateObserver;
  }

  public CacheKeyFactory getCacheKeyFactory() {
    return mCacheKeyFactory;
  }

  public Context getContext() {
    return mContext;
  }

  public static DefaultImageRequestConfig getDefaultImageRequestConfig() {
    return sDefaultImageRequestConfig;
  }

  public FileCacheFactory getFileCacheFactory() {
    return mFileCacheFactory;
  }

  public boolean isDownsampleEnabled() {
    return mDownsampleEnabled;
  }

  public boolean isDiskCacheEnabled() {
    return mDiskCacheEnabled;
  }

  public Supplier<MemoryCacheParams> getEncodedMemoryCacheParamsSupplier() {
    return mEncodedMemoryCacheParamsSupplier;
  }

  public ExecutorSupplier getExecutorSupplier() {
    return mExecutorSupplier;
  }

  public ImageCacheStatsTracker getImageCacheStatsTracker() {
    return mImageCacheStatsTracker;
  }

  @Nullable
  public ImageDecoder getImageDecoder() {
    return mImageDecoder;
  }

  @Nullable
  public ImageTranscoderFactory getImageTranscoderFactory() {
    return mImageTranscoderFactory;
  }

  @Nullable
  @ImageTranscoderType
  public Integer getImageTranscoderType() {
    return mImageTranscoderType;
  }

  public Supplier<Boolean> getIsPrefetchEnabledSupplier() {
    return mIsPrefetchEnabledSupplier;
  }

  public DiskCacheConfig getMainDiskCacheConfig() {
    return mMainDiskCacheConfig;
  }

  public MemoryTrimmableRegistry getMemoryTrimmableRegistry() {
    return mMemoryTrimmableRegistry;
  }

  @MemoryChunkType
  public int getMemoryChunkType() {
    return mMemoryChunkType;
  }

  public NetworkFetcher getNetworkFetcher() {
    return mNetworkFetcher;
  }

  @Nullable
  public PlatformBitmapFactory getPlatformBitmapFactory() {
    return mPlatformBitmapFactory;
  }

  public PoolFactory getPoolFactory() {
    return mPoolFactory;
  }

  public ProgressiveJpegConfig getProgressiveJpegConfig() {
    return mProgressiveJpegConfig;
  }

  public Set<RequestListener> getRequestListeners() {
    return Collections.unmodifiableSet(mRequestListeners);
  }

  public Set<RequestListener2> getRequestListener2s() {
    return Collections.unmodifiableSet(mRequestListener2s);
  }

  public boolean isResizeAndRotateEnabledForNetwork() {
    return mResizeAndRotateEnabledForNetwork;
  }

  public DiskCacheConfig getSmallImageDiskCacheConfig() {
    return mSmallImageDiskCacheConfig;
  }

  @Nullable
  public ImageDecoderConfig getImageDecoderConfig() {
    return mImageDecoderConfig;
  }

  @Nullable
  public CallerContextVerifier getCallerContextVerifier() {
    return mCallerContextVerifier;
  }

  public ImagePipelineExperiments getExperiments() {
    return mImagePipelineExperiments;
  }

  public CloseableReferenceLeakTracker getCloseableReferenceLeakTracker() {
    return mCloseableReferenceLeakTracker;
  }

  public static Builder newBuilder(Context context) {
    return new Builder(context);
  }

  @Nullable
  private static ImageTranscoderFactory getImageTranscoderFactory(final Builder builder) {
    if (builder.mImageTranscoderFactory != null && builder.mImageTranscoderType != null) {
      throw new IllegalStateException(
          "You can't define a custom ImageTranscoderFactory and provide an ImageTranscoderType");
    }
    if (builder.mImageTranscoderFactory != null) {
      return builder.mImageTranscoderFactory;
    } else {
      return null; // This member will be constructed by ImagePipelineFactory
    }
  }

  @MemoryChunkType
  private static int getMemoryChunkType(
      final Builder builder, final ImagePipelineExperiments imagePipelineExperiments) {
    if (builder.mMemoryChunkType != null) {
      return builder.mMemoryChunkType;
    } else if (imagePipelineExperiments.getMemoryType() == MemoryChunkType.ASHMEM_MEMORY
        && Build.VERSION.SDK_INT >= 27) {
      return MemoryChunkType.ASHMEM_MEMORY;
    } else if (imagePipelineExperiments.getMemoryType() == MemoryChunkType.BUFFER_MEMORY) {
      return MemoryChunkType.BUFFER_MEMORY;
    } else if (imagePipelineExperiments.getMemoryType() == MemoryChunkType.NATIVE_MEMORY) {
      return MemoryChunkType.NATIVE_MEMORY;
    } else {
      return MemoryChunkType.NATIVE_MEMORY;
    }
  }

  @Nullable
  public MemoryCache<CacheKey, CloseableImage> getBitmapCacheOverride() {
    return mBitmapCache;
  }

  @Nullable
  public MemoryCache<CacheKey, PooledByteBuffer> getEncodedMemoryCacheOverride() {
    return mEncodedMemoryCache;
  }

  public BitmapMemoryCacheFactory getBitmapMemoryCacheFactory() {
    return mBitmapMemoryCacheFactory;
  }

  /** Contains default configuration that can be personalized for all the request */
  public static class DefaultImageRequestConfig {

    private boolean mProgressiveRenderingEnabled = false;

    private DefaultImageRequestConfig() {}

    public void setProgressiveRenderingEnabled(boolean progressiveRenderingEnabled) {
      this.mProgressiveRenderingEnabled = progressiveRenderingEnabled;
    }

    public boolean isProgressiveRenderingEnabled() {
      return mProgressiveRenderingEnabled;
    }
  }

  public static class Builder {

    private Bitmap.Config mBitmapConfig;
    private Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;
    private CountingMemoryCache.EntryStateObserver<CacheKey> mBitmapMemoryCacheEntryStateObserver;
    private MemoryCache.CacheTrimStrategy mBitmapMemoryCacheTrimStrategy;
    private CacheKeyFactory mCacheKeyFactory;
    private final Context mContext;
    private boolean mDownsampleEnabled = false;
    private Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;
    private ExecutorSupplier mExecutorSupplier;
    private ImageCacheStatsTracker mImageCacheStatsTracker;
    private ImageDecoder mImageDecoder;
    private ImageTranscoderFactory mImageTranscoderFactory;
    @Nullable @ImageTranscoderType private Integer mImageTranscoderType = null;
    private Supplier<Boolean> mIsPrefetchEnabledSupplier;
    private DiskCacheConfig mMainDiskCacheConfig;
    private MemoryTrimmableRegistry mMemoryTrimmableRegistry;
    @Nullable @MemoryChunkType private Integer mMemoryChunkType = null;
    private NetworkFetcher mNetworkFetcher;
    private PlatformBitmapFactory mPlatformBitmapFactory;
    private PoolFactory mPoolFactory;
    private ProgressiveJpegConfig mProgressiveJpegConfig;
    private Set<RequestListener> mRequestListeners;
    private Set<RequestListener2> mRequestListener2s;
    private boolean mResizeAndRotateEnabledForNetwork = true;
    private DiskCacheConfig mSmallImageDiskCacheConfig;
    private FileCacheFactory mFileCacheFactory;
    private ImageDecoderConfig mImageDecoderConfig;
    private int mHttpConnectionTimeout = -1;
    private final ImagePipelineExperiments.Builder mExperimentsBuilder =
        new ImagePipelineExperiments.Builder(this);
    private boolean mDiskCacheEnabled = true;
    private CallerContextVerifier mCallerContextVerifier;
    private CloseableReferenceLeakTracker mCloseableReferenceLeakTracker =
        new NoOpCloseableReferenceLeakTracker();
    @Nullable private MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
    @Nullable private MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
    @Nullable private BitmapMemoryCacheFactory mBitmapMemoryCacheFactory;

    private Builder(Context context) {
      // Doesn't use a setter as always required.
      mContext = Preconditions.checkNotNull(context);
    }

    public Builder setBitmapsConfig(Bitmap.Config config) {
      mBitmapConfig = config;
      return this;
    }

    public Builder setBitmapMemoryCacheParamsSupplier(
        Supplier<MemoryCacheParams> bitmapMemoryCacheParamsSupplier) {
      mBitmapMemoryCacheParamsSupplier =
          Preconditions.checkNotNull(bitmapMemoryCacheParamsSupplier);
      return this;
    }

    public Builder setBitmapMemoryCacheEntryStateObserver(
        CountingMemoryCache.EntryStateObserver<CacheKey> bitmapMemoryCacheEntryStateObserver) {
      mBitmapMemoryCacheEntryStateObserver = bitmapMemoryCacheEntryStateObserver;
      return this;
    }

    public Builder setBitmapMemoryCacheTrimStrategy(MemoryCache.CacheTrimStrategy trimStrategy) {
      mBitmapMemoryCacheTrimStrategy = trimStrategy;
      return this;
    }

    public Builder setCacheKeyFactory(CacheKeyFactory cacheKeyFactory) {
      mCacheKeyFactory = cacheKeyFactory;
      return this;
    }

    public Builder setHttpConnectionTimeout(int httpConnectionTimeoutMs) {
      mHttpConnectionTimeout = httpConnectionTimeoutMs;
      return this;
    }

    public Builder setFileCacheFactory(FileCacheFactory fileCacheFactory) {
      mFileCacheFactory = fileCacheFactory;
      return this;
    }

    public boolean isDownsampleEnabled() {
      return mDownsampleEnabled;
    }

    public Builder setDownsampleEnabled(boolean downsampleEnabled) {
      mDownsampleEnabled = downsampleEnabled;
      return this;
    }

    public boolean isDiskCacheEnabled() {
      return mDiskCacheEnabled;
    }

    public Builder setDiskCacheEnabled(boolean diskCacheEnabled) {
      mDiskCacheEnabled = diskCacheEnabled;
      return this;
    }

    public Builder setEncodedMemoryCacheParamsSupplier(
        Supplier<MemoryCacheParams> encodedMemoryCacheParamsSupplier) {
      mEncodedMemoryCacheParamsSupplier =
          Preconditions.checkNotNull(encodedMemoryCacheParamsSupplier);
      return this;
    }

    public Builder setExecutorSupplier(ExecutorSupplier executorSupplier) {
      mExecutorSupplier = executorSupplier;
      return this;
    }

    public Builder setImageCacheStatsTracker(ImageCacheStatsTracker imageCacheStatsTracker) {
      mImageCacheStatsTracker = imageCacheStatsTracker;
      return this;
    }

    public Builder setImageDecoder(ImageDecoder imageDecoder) {
      mImageDecoder = imageDecoder;
      return this;
    }

    @Nullable
    @ImageTranscoderType
    public Integer getImageTranscoderType() {
      return mImageTranscoderType;
    }

    public Builder setImageTranscoderType(@ImageTranscoderType int imageTranscoderType) {
      mImageTranscoderType = imageTranscoderType;
      return this;
    }

    public Builder setImageTranscoderFactory(ImageTranscoderFactory imageTranscoderFactory) {
      mImageTranscoderFactory = imageTranscoderFactory;
      return this;
    }

    public Builder setIsPrefetchEnabledSupplier(Supplier<Boolean> isPrefetchEnabledSupplier) {
      mIsPrefetchEnabledSupplier = isPrefetchEnabledSupplier;
      return this;
    }

    public Builder setMainDiskCacheConfig(DiskCacheConfig mainDiskCacheConfig) {
      mMainDiskCacheConfig = mainDiskCacheConfig;
      return this;
    }

    public Builder setMemoryTrimmableRegistry(MemoryTrimmableRegistry memoryTrimmableRegistry) {
      mMemoryTrimmableRegistry = memoryTrimmableRegistry;
      return this;
    }

    @Nullable
    @MemoryChunkType
    public Integer getMemoryChunkType() {
      return mMemoryChunkType;
    }

    public Builder setMemoryChunkType(@MemoryChunkType int memoryChunkType) {
      mMemoryChunkType = memoryChunkType;
      return this;
    }

    public Builder setNetworkFetcher(NetworkFetcher networkFetcher) {
      mNetworkFetcher = networkFetcher;
      return this;
    }

    public Builder setPlatformBitmapFactory(PlatformBitmapFactory platformBitmapFactory) {
      mPlatformBitmapFactory = platformBitmapFactory;
      return this;
    }

    public Builder setPoolFactory(PoolFactory poolFactory) {
      mPoolFactory = poolFactory;
      return this;
    }

    public Builder setProgressiveJpegConfig(ProgressiveJpegConfig progressiveJpegConfig) {
      mProgressiveJpegConfig = progressiveJpegConfig;
      return this;
    }

    public Builder setRequestListeners(Set<RequestListener> requestListeners) {
      mRequestListeners = requestListeners;
      return this;
    }

    public Builder setRequestListener2s(Set<RequestListener2> requestListeners) {
      mRequestListener2s = requestListeners;
      return this;
    }

    public Builder setResizeAndRotateEnabledForNetwork(boolean resizeAndRotateEnabledForNetwork) {
      mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
      return this;
    }

    public Builder setSmallImageDiskCacheConfig(DiskCacheConfig smallImageDiskCacheConfig) {
      mSmallImageDiskCacheConfig = smallImageDiskCacheConfig;
      return this;
    }

    public Builder setImageDecoderConfig(ImageDecoderConfig imageDecoderConfig) {
      mImageDecoderConfig = imageDecoderConfig;
      return this;
    }

    public Builder setCallerContextVerifier(CallerContextVerifier callerContextVerifier) {
      mCallerContextVerifier = callerContextVerifier;
      return this;
    }

    public Builder setCloseableReferenceLeakTracker(
        CloseableReferenceLeakTracker closeableReferenceLeakTracker) {
      mCloseableReferenceLeakTracker = closeableReferenceLeakTracker;
      return this;
    }

    public Builder setBitmapMemoryCache(
        @Nullable MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache) {
      mBitmapMemoryCache = bitmapMemoryCache;
      return this;
    }

    public Builder setEncodedMemoryCache(
        @Nullable MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache) {
      mEncodedMemoryCache = encodedMemoryCache;
      return this;
    }

    public Builder setBitmapMemoryCacheFactory(
        @Nullable BitmapMemoryCacheFactory bitmapMemoryCacheFactory) {
      mBitmapMemoryCacheFactory = bitmapMemoryCacheFactory;
      return this;
    }

    public ImagePipelineExperiments.Builder experiment() {
      return mExperimentsBuilder;
    }

    public ImagePipelineConfig build() {
      return new ImagePipelineConfig(this);
    }
  }
}
