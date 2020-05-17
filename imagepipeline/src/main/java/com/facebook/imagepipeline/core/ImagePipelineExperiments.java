/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import android.content.Context;
import android.graphics.Bitmap;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.webp.WebpBitmapFactory;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imageutils.BitmapUtil;

/**
 * Encapsulates additional elements of the {@link ImagePipelineConfig} which are currently in an
 * experimental state.
 *
 * <p>These options may often change or disappear altogether and it is not recommended to change
 * their values from their defaults.
 */
public class ImagePipelineExperiments {

  private final boolean mWebpSupportEnabled;
  private final WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;
  private final boolean mDecodeCancellationEnabled;
  private final WebpBitmapFactory mWebpBitmapFactory;
  private final boolean mUseDownsamplingRatioForResizing;
  private final boolean mUseBitmapPrepareToDraw;
  private final int mBitmapPrepareToDrawMinSizeBytes;
  private final int mBitmapPrepareToDrawMaxSizeBytes;
  private boolean mBitmapPrepareToDrawForPrefetch;
  private final int mMaxBitmapSize;
  private final boolean mNativeCodeDisabled;
  private final boolean mPartialImageCachingEnabled;
  private final ProducerFactoryMethod mProducerFactoryMethod;
  private final Supplier<Boolean> mLazyDataSource;
  private final boolean mGingerbreadDecoderEnabled;
  private final boolean mDownscaleFrameToDrawableDimensions;
  private final int mBitmapCloseableRefType;
  private final Supplier<Boolean> mSuppressBitmapPrefetchingSupplier;
  private final boolean mExperimentalThreadHandoffQueueEnabled;
  private final long mMemoryType;
  private boolean mKeepCancelledFetchAsLowPriority;
  private boolean mDownsampleIfLargeBitmap;
  private boolean mEncodedCacheEnabled;
  private final boolean mEnsureTranscoderLibraryLoaded;
  private final boolean mIsEncodedMemoryCacheProbingEnabled;
  private final boolean mIsDiskCacheProbingEnabled;
  private final int mTrackedKeysSize;

  private ImagePipelineExperiments(Builder builder) {
    mWebpSupportEnabled = builder.mWebpSupportEnabled;
    mWebpErrorLogger = builder.mWebpErrorLogger;
    mDecodeCancellationEnabled = builder.mDecodeCancellationEnabled;
    mWebpBitmapFactory = builder.mWebpBitmapFactory;
    mUseDownsamplingRatioForResizing = builder.mUseDownsamplingRatioForResizing;
    mUseBitmapPrepareToDraw = builder.mUseBitmapPrepareToDraw;
    mBitmapPrepareToDrawMinSizeBytes = builder.mBitmapPrepareToDrawMinSizeBytes;
    mBitmapPrepareToDrawMaxSizeBytes = builder.mBitmapPrepareToDrawMaxSizeBytes;
    mBitmapPrepareToDrawForPrefetch = builder.mBitmapPrepareToDrawForPrefetch;
    mMaxBitmapSize = builder.mMaxBitmapSize;
    mNativeCodeDisabled = builder.mNativeCodeDisabled;
    mPartialImageCachingEnabled = builder.mPartialImageCachingEnabled;
    if (builder.mProducerFactoryMethod == null) {
      mProducerFactoryMethod = new DefaultProducerFactoryMethod();
    } else {
      mProducerFactoryMethod = builder.mProducerFactoryMethod;
    }
    mLazyDataSource = builder.mLazyDataSource;
    mGingerbreadDecoderEnabled = builder.mGingerbreadDecoderEnabled;
    mDownscaleFrameToDrawableDimensions = builder.mDownscaleFrameToDrawableDimensions;
    mBitmapCloseableRefType = builder.mBitmapCloseableRefType;
    mSuppressBitmapPrefetchingSupplier = builder.mSuppressBitmapPrefetchingSupplier;
    mExperimentalThreadHandoffQueueEnabled = builder.mExperimentalThreadHandoffQueueEnabled;
    mMemoryType = builder.mMemoryType;
    mKeepCancelledFetchAsLowPriority = builder.mKeepCancelledFetchAsLowPriority;
    mDownsampleIfLargeBitmap = builder.mDownsampleIfLargeBitmap;
    mEncodedCacheEnabled = builder.mEncodedCacheEnabled;
    mEnsureTranscoderLibraryLoaded = builder.mEnsureTranscoderLibraryLoaded;
    mIsEncodedMemoryCacheProbingEnabled = builder.mIsEncodedMemoryCacheProbingEnabled;
    mIsDiskCacheProbingEnabled = builder.mIsDiskCacheProbingEnabled;
    mTrackedKeysSize = builder.mTrackedKeysSize;
  }

  public boolean isEncodedCacheEnabled() {
    return mEncodedCacheEnabled;
  }

  public boolean shouldDownsampleIfLargeBitmap() {
    return mDownsampleIfLargeBitmap;
  }

  public boolean getUseDownsamplingRatioForResizing() {
    return mUseDownsamplingRatioForResizing;
  }

  public boolean isWebpSupportEnabled() {
    return mWebpSupportEnabled;
  }

  public boolean isDecodeCancellationEnabled() {
    return mDecodeCancellationEnabled;
  }

  public WebpBitmapFactory.WebpErrorLogger getWebpErrorLogger() {
    return mWebpErrorLogger;
  }

  public WebpBitmapFactory getWebpBitmapFactory() {
    return mWebpBitmapFactory;
  }

  public boolean getUseBitmapPrepareToDraw() {
    return mUseBitmapPrepareToDraw;
  }

  public int getBitmapPrepareToDrawMinSizeBytes() {
    return mBitmapPrepareToDrawMinSizeBytes;
  }

  public int getBitmapPrepareToDrawMaxSizeBytes() {
    return mBitmapPrepareToDrawMaxSizeBytes;
  }

  public boolean isNativeCodeDisabled() {
    return mNativeCodeDisabled;
  }

  public boolean isPartialImageCachingEnabled() {
    return mPartialImageCachingEnabled;
  }

  public ProducerFactoryMethod getProducerFactoryMethod() {
    return mProducerFactoryMethod;
  }

  public boolean isExperimentalThreadHandoffQueueEnabled() {
    return mExperimentalThreadHandoffQueueEnabled;
  }

  public static ImagePipelineExperiments.Builder newBuilder(
      ImagePipelineConfig.Builder configBuilder) {
    return new ImagePipelineExperiments.Builder(configBuilder);
  }

  public boolean getBitmapPrepareToDrawForPrefetch() {
    return mBitmapPrepareToDrawForPrefetch;
  }

  public int getMaxBitmapSize() {
    return mMaxBitmapSize;
  }

  public Supplier<Boolean> isLazyDataSource() {
    return mLazyDataSource;
  }

  public boolean isEnsureTranscoderLibraryLoaded() {
    return mEnsureTranscoderLibraryLoaded;
  }

  public boolean isDiskCacheProbingEnabled() {
    return mIsDiskCacheProbingEnabled;
  }

  public boolean isEncodedMemoryCacheProbingEnabled() {
    return mIsEncodedMemoryCacheProbingEnabled;
  }

  public boolean isGingerbreadDecoderEnabled() {
    return mGingerbreadDecoderEnabled;
  }

  public boolean shouldDownscaleFrameToDrawableDimensions() {
    return mDownscaleFrameToDrawableDimensions;
  }

  public int getBitmapCloseableRefType() {
    return mBitmapCloseableRefType;
  }

  public Supplier<Boolean> getSuppressBitmapPrefetchingSupplier() {
    return mSuppressBitmapPrefetchingSupplier;
  }

  public long getMemoryType() {
    return mMemoryType;
  }

  public int getTrackedKeysSize() {
    return mTrackedKeysSize;
  }

  public boolean shouldKeepCancelledFetchAsLowPriority() {
    return mKeepCancelledFetchAsLowPriority;
  }

  public static class Builder {

    private final ImagePipelineConfig.Builder mConfigBuilder;
    private boolean mWebpSupportEnabled = false;
    private WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;
    private boolean mDecodeCancellationEnabled = false;
    private WebpBitmapFactory mWebpBitmapFactory;
    private boolean mUseDownsamplingRatioForResizing = false;
    private boolean mUseBitmapPrepareToDraw = false;
    private int mBitmapPrepareToDrawMinSizeBytes = 0;
    private int mBitmapPrepareToDrawMaxSizeBytes = 0;
    public boolean mBitmapPrepareToDrawForPrefetch = false;
    private int mMaxBitmapSize = (int) BitmapUtil.MAX_BITMAP_SIZE;
    private boolean mNativeCodeDisabled = false;
    private boolean mPartialImageCachingEnabled = false;
    private ProducerFactoryMethod mProducerFactoryMethod;
    public Supplier<Boolean> mLazyDataSource;
    public boolean mGingerbreadDecoderEnabled;
    public boolean mDownscaleFrameToDrawableDimensions;
    public int mBitmapCloseableRefType;
    public Supplier<Boolean> mSuppressBitmapPrefetchingSupplier = Suppliers.of(false);
    public boolean mExperimentalThreadHandoffQueueEnabled;
    public long mMemoryType = 0;
    private boolean mKeepCancelledFetchAsLowPriority;
    public boolean mDownsampleIfLargeBitmap;
    public boolean mEncodedCacheEnabled = true;
    public boolean mEnsureTranscoderLibraryLoaded = true;
    private boolean mIsEncodedMemoryCacheProbingEnabled = false;
    private boolean mIsDiskCacheProbingEnabled = false;
    private int mTrackedKeysSize = 20;

    public Builder(ImagePipelineConfig.Builder configBuilder) {
      mConfigBuilder = configBuilder;
    }

    public ImagePipelineConfig.Builder setWebpSupportEnabled(boolean webpSupportEnabled) {
      mWebpSupportEnabled = webpSupportEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setUseDownsampligRatioForResizing(
        boolean useDownsamplingRatioForResizing) {
      mUseDownsamplingRatioForResizing = useDownsamplingRatioForResizing;
      return mConfigBuilder;
    }

    /**
     * Enables the caching of partial image data, for example if the request is cancelled or fails
     * after some data has been received.
     */
    public ImagePipelineConfig.Builder setPartialImageCachingEnabled(
        boolean partialImageCachingEnabled) {
      mPartialImageCachingEnabled = partialImageCachingEnabled;
      return mConfigBuilder;
    }

    public boolean isPartialImageCachingEnabled() {
      return mPartialImageCachingEnabled;
    }

    /**
     * If true we cancel decoding jobs when the related request has been cancelled
     *
     * @param decodeCancellationEnabled If true the decoding of cancelled requests are cancelled
     * @return The Builder itself for chaining
     */
    public ImagePipelineConfig.Builder setDecodeCancellationEnabled(
        boolean decodeCancellationEnabled) {
      mDecodeCancellationEnabled = decodeCancellationEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setWebpErrorLogger(
        WebpBitmapFactory.WebpErrorLogger webpErrorLogger) {
      mWebpErrorLogger = webpErrorLogger;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setWebpBitmapFactory(WebpBitmapFactory webpBitmapFactory) {
      mWebpBitmapFactory = webpBitmapFactory;
      return mConfigBuilder;
    }

    /**
     * If enabled, the pipeline will call {@link android.graphics.Bitmap#prepareToDraw()} after
     * decoding. This potentially reduces lag on Android N+ as this step now happens async when the
     * RendererThread is idle.
     *
     * @param useBitmapPrepareToDraw set true for enabling prepareToDraw
     * @param minBitmapSizeBytes Bitmaps with a {@link Bitmap#getByteCount()} smaller than this
     *     value are not uploaded
     * @param maxBitmapSizeBytes Bitmaps with a {@link Bitmap#getByteCount()} larger than this value
     *     are not uploaded
     * @param preparePrefetch If this is true, also pre-fetching image requests will trigger the
     *     {@link android.graphics.Bitmap#prepareToDraw()} call.
     * @return The Builder itself for chaining
     */
    public ImagePipelineConfig.Builder setBitmapPrepareToDraw(
        boolean useBitmapPrepareToDraw,
        int minBitmapSizeBytes,
        int maxBitmapSizeBytes,
        boolean preparePrefetch) {
      mUseBitmapPrepareToDraw = useBitmapPrepareToDraw;
      mBitmapPrepareToDrawMinSizeBytes = minBitmapSizeBytes;
      mBitmapPrepareToDrawMaxSizeBytes = maxBitmapSizeBytes;
      mBitmapPrepareToDrawForPrefetch = preparePrefetch;
      return mConfigBuilder;
    }

    /**
     * Sets the maximum bitmap size use to compute the downsampling value when decoding Jpeg images.
     */
    public ImagePipelineConfig.Builder setMaxBitmapSize(int maxBitmapSize) {
      mMaxBitmapSize = maxBitmapSize;
      return mConfigBuilder;
    }

    /**
     * If true, the pipeline will use alternative implementations without native code.
     *
     * @param nativeCodeDisabled set true for disabling native implementation.
     * @return The Builder itself for chaining
     */
    public ImagePipelineConfig.Builder setNativeCodeDisabled(boolean nativeCodeDisabled) {
      mNativeCodeDisabled = nativeCodeDisabled;
      return mConfigBuilder;
    }

    /**
     * Stores an alternative method to instantiate the {@link ProducerFactory}. This allows
     * experimenting with overridden producers.
     */
    public ImagePipelineConfig.Builder setProducerFactoryMethod(
        ProducerFactoryMethod producerFactoryMethod) {
      mProducerFactoryMethod = producerFactoryMethod;
      return mConfigBuilder;
    }

    /** Stores an alternative lazy method to instantiate the data souce. */
    public ImagePipelineConfig.Builder setLazyDataSource(Supplier<Boolean> lazyDataSource) {
      mLazyDataSource = lazyDataSource;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setGingerbreadDecoderEnabled(
        boolean gingerbreadDecoderEnabled) {
      mGingerbreadDecoderEnabled = gingerbreadDecoderEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setShouldDownscaleFrameToDrawableDimensions(
        boolean downscaleFrameToDrawableDimensions) {
      mDownscaleFrameToDrawableDimensions = downscaleFrameToDrawableDimensions;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setBitmapCloseableRefType(int bitmapCloseableRefType) {
      mBitmapCloseableRefType = bitmapCloseableRefType;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setSuppressBitmapPrefetchingSupplier(
        Supplier<Boolean> suppressBitmapPrefetchingSupplier) {
      mSuppressBitmapPrefetchingSupplier = suppressBitmapPrefetchingSupplier;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setExperimentalThreadHandoffQueueEnabled(
        boolean experimentalThreadHandoffQueueEnabled) {
      mExperimentalThreadHandoffQueueEnabled = experimentalThreadHandoffQueueEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setExperimentalMemoryType(long MemoryType) {
      mMemoryType = MemoryType;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setKeepCancelledFetchAsLowPriority(
        boolean keepCancelledFetchAsLowPriority) {
      mKeepCancelledFetchAsLowPriority = keepCancelledFetchAsLowPriority;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setDownsampleIfLargeBitmap(boolean downsampleIfLargeBitmap) {
      mDownsampleIfLargeBitmap = downsampleIfLargeBitmap;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setEncodedCacheEnabled(boolean encodedCacheEnabled) {
      mEncodedCacheEnabled = encodedCacheEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setEnsureTranscoderLibraryLoaded(
        boolean ensureTranscoderLibraryLoaded) {
      mEnsureTranscoderLibraryLoaded = ensureTranscoderLibraryLoaded;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setIsDiskCacheProbingEnabled(
        boolean isDiskCacheProbingEnabled) {
      mIsDiskCacheProbingEnabled = isDiskCacheProbingEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setIsEncodedMemoryCacheProbingEnabled(
        boolean isEncodedMemoryCacheProbingEnabled) {
      mIsEncodedMemoryCacheProbingEnabled = isEncodedMemoryCacheProbingEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setTrackedKeysSize(int trackedKeysSize) {
      mTrackedKeysSize = trackedKeysSize;
      return mConfigBuilder;
    }

    public ImagePipelineExperiments build() {
      return new ImagePipelineExperiments(this);
    }
  }

  public interface ProducerFactoryMethod {

    ProducerFactory createProducerFactory(
        Context context,
        ByteArrayPool byteArrayPool,
        ImageDecoder imageDecoder,
        ProgressiveJpegConfig progressiveJpegConfig,
        boolean downsampleEnabled,
        boolean resizeAndRotateEnabledForNetwork,
        boolean decodeCancellationEnabled,
        ExecutorSupplier executorSupplier,
        PooledByteBufferFactory pooledByteBufferFactory,
        MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache,
        MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache,
        BufferedDiskCache defaultBufferedDiskCache,
        BufferedDiskCache smallImageBufferedDiskCache,
        CacheKeyFactory cacheKeyFactory,
        PlatformBitmapFactory platformBitmapFactory,
        int bitmapPrepareToDrawMinSizeBytes,
        int bitmapPrepareToDrawMaxSizeBytes,
        boolean bitmapPrepareToDrawForPrefetch,
        int maxBitmapSize,
        CloseableReferenceFactory closeableReferenceFactory,
        boolean keepCancelledFetchAsLowPriority,
        int trackedKeysSize);
  }

  public static class DefaultProducerFactoryMethod implements ProducerFactoryMethod {

    @Override
    public ProducerFactory createProducerFactory(
        Context context,
        ByteArrayPool byteArrayPool,
        ImageDecoder imageDecoder,
        ProgressiveJpegConfig progressiveJpegConfig,
        boolean downsampleEnabled,
        boolean resizeAndRotateEnabledForNetwork,
        boolean decodeCancellationEnabled,
        ExecutorSupplier executorSupplier,
        PooledByteBufferFactory pooledByteBufferFactory,
        MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache,
        MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache,
        BufferedDiskCache defaultBufferedDiskCache,
        BufferedDiskCache smallImageBufferedDiskCache,
        CacheKeyFactory cacheKeyFactory,
        PlatformBitmapFactory platformBitmapFactory,
        int bitmapPrepareToDrawMinSizeBytes,
        int bitmapPrepareToDrawMaxSizeBytes,
        boolean bitmapPrepareToDrawForPrefetch,
        int maxBitmapSize,
        CloseableReferenceFactory closeableReferenceFactory,
        boolean keepCancelledFetchAsLowPriority,
        int trackedKeysSize) {
      return new ProducerFactory(
          context,
          byteArrayPool,
          imageDecoder,
          progressiveJpegConfig,
          downsampleEnabled,
          resizeAndRotateEnabledForNetwork,
          decodeCancellationEnabled,
          executorSupplier,
          pooledByteBufferFactory,
          bitmapMemoryCache,
          encodedMemoryCache,
          defaultBufferedDiskCache,
          smallImageBufferedDiskCache,
          cacheKeyFactory,
          platformBitmapFactory,
          bitmapPrepareToDrawMinSizeBytes,
          bitmapPrepareToDrawMaxSizeBytes,
          bitmapPrepareToDrawForPrefetch,
          maxBitmapSize,
          closeableReferenceFactory,
          keepCancelledFetchAsLowPriority,
          trackedKeysSize);
    }
  }
}
