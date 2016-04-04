/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry;
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactory;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.DefaultBitmapMemoryCacheParamsSupplier;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.cache.DefaultEncodedMemoryCacheParamsSupplier;
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.cache.NoOpImageCacheStatsTracker;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.HttpUrlConnectionNetworkFetcher;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import static com.facebook.common.webp.WebpSupportStatus.sWebpLibraryPresent;

/**
 * Master configuration class for the image pipeline library.
 *
 * To use:
 * <code>
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
  @Nullable private final AnimatedImageFactory mAnimatedImageFactory;
  private final Bitmap.Config mBitmapConfig;
  private final Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Context mContext;
  private final boolean mDownsampleEnabled;
  private final boolean mWebpSupportEnabled;
  private final boolean mDecodeFileDescriptorEnabled;
  private final boolean mDecodeMemoryFileEnabled;
  private final FileCacheFactory mFileCacheFactory;
  private final Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;
  private final ExecutorSupplier mExecutorSupplier;
  private final int mForceSmallCacheThresholdBytes;
  private final ImageCacheStatsTracker mImageCacheStatsTracker;
  @Nullable private final ImageDecoder mImageDecoder;
  private final Supplier<Boolean> mIsPrefetchEnabledSupplier;
  private final DiskCacheConfig mMainDiskCacheConfig;
  private final MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  private final NetworkFetcher mNetworkFetcher;
  @Nullable private final PlatformBitmapFactory mPlatformBitmapFactory;
  private final PoolFactory mPoolFactory;
  private final ProgressiveJpegConfig mProgressiveJpegConfig;
  private final Set<RequestListener> mRequestListeners;
  private final boolean mResizeAndRotateEnabledForNetwork;
  private final DiskCacheConfig mSmallImageDiskCacheConfig;

  private ImagePipelineConfig(Builder builder) {
    mAnimatedImageFactory = builder.mAnimatedImageFactory;
    mBitmapMemoryCacheParamsSupplier =
        builder.mBitmapMemoryCacheParamsSupplier == null ?
            new DefaultBitmapMemoryCacheParamsSupplier(
                (ActivityManager) builder.mContext.getSystemService(Context.ACTIVITY_SERVICE)) :
            builder.mBitmapMemoryCacheParamsSupplier;
    mBitmapConfig =
        builder.mBitmapConfig == null ?
            Bitmap.Config.ARGB_8888 :
            builder.mBitmapConfig;
    mCacheKeyFactory =
        builder.mCacheKeyFactory == null ?
            DefaultCacheKeyFactory.getInstance() :
            builder.mCacheKeyFactory;
    mContext = Preconditions.checkNotNull(builder.mContext);
    mDecodeFileDescriptorEnabled = builder.mDownsampleEnabled &&
        builder.mDecodeFileDescriptorEnabled;
    mDecodeMemoryFileEnabled = builder.mDecodeMemoryFileEnabled;
    mFileCacheFactory = builder.mFileCacheFactory == null ?
        new DiskStorageCacheFactory(new DynamicDefaultDiskStorageFactory()) :
        builder.mFileCacheFactory;
    mDownsampleEnabled = builder.mDownsampleEnabled;
    mWebpSupportEnabled = builder.mWebpSupportEnabled && sWebpLibraryPresent;
    mEncodedMemoryCacheParamsSupplier =
        builder.mEncodedMemoryCacheParamsSupplier == null ?
            new DefaultEncodedMemoryCacheParamsSupplier() :
            builder.mEncodedMemoryCacheParamsSupplier;
    mForceSmallCacheThresholdBytes = builder.mForceSmallCacheThresholdBytes;
    mImageCacheStatsTracker =
        builder.mImageCacheStatsTracker == null ?
            NoOpImageCacheStatsTracker.getInstance() :
            builder.mImageCacheStatsTracker;
    mImageDecoder = builder.mImageDecoder;
    mIsPrefetchEnabledSupplier =
        builder.mIsPrefetchEnabledSupplier == null ?
            new Supplier<Boolean>() {
              @Override
              public Boolean get() {
                return true;
              }
            } :
            builder.mIsPrefetchEnabledSupplier;
    mMainDiskCacheConfig =
        builder.mMainDiskCacheConfig == null ?
            getDefaultMainDiskCacheConfig(builder.mContext) :
            builder.mMainDiskCacheConfig;
    mMemoryTrimmableRegistry =
        builder.mMemoryTrimmableRegistry == null ?
            NoOpMemoryTrimmableRegistry.getInstance() :
            builder.mMemoryTrimmableRegistry;
    mNetworkFetcher =
        builder.mNetworkFetcher == null ?
            new HttpUrlConnectionNetworkFetcher() :
            builder.mNetworkFetcher;
    mPlatformBitmapFactory = builder.mPlatformBitmapFactory;
    mPoolFactory =
        builder.mPoolFactory == null ?
            new PoolFactory(PoolConfig.newBuilder().build()) :
            builder.mPoolFactory;
    mProgressiveJpegConfig =
        builder.mProgressiveJpegConfig == null ?
            new SimpleProgressiveJpegConfig() :
            builder.mProgressiveJpegConfig;
    mRequestListeners =
        builder.mRequestListeners == null ?
            new HashSet<RequestListener>() :
            builder.mRequestListeners;
    mResizeAndRotateEnabledForNetwork = builder.mResizeAndRotateEnabledForNetwork;
    mSmallImageDiskCacheConfig =
        builder.mSmallImageDiskCacheConfig == null ?
            mMainDiskCacheConfig :
            builder.mSmallImageDiskCacheConfig;

    // Below this comment can't be built in alphabetical order, because of dependencies
    int numCpuBoundThreads = mPoolFactory.getFlexByteArrayPoolMaxNumThreads();
    mExecutorSupplier =
        builder.mExecutorSupplier == null ?
            new DefaultExecutorSupplier(numCpuBoundThreads) : builder.mExecutorSupplier;
  }

  private static DiskCacheConfig getDefaultMainDiskCacheConfig(final Context context) {
    return DiskCacheConfig.newBuilder(context).build();
  }

  @Nullable
  public AnimatedImageFactory getAnimatedImageFactory() {
    return mAnimatedImageFactory;
  }

  public Bitmap.Config getBitmapConfig() {
    return mBitmapConfig;
  }

  public Supplier<MemoryCacheParams> getBitmapMemoryCacheParamsSupplier() {
    return mBitmapMemoryCacheParamsSupplier;
  }

  public CacheKeyFactory getCacheKeyFactory() {
    return mCacheKeyFactory;
  }

  public Context getContext() {
    return mContext;
  }

  public boolean isDecodeFileDescriptorEnabled() {
    return mDecodeFileDescriptorEnabled;
  }

  public boolean isDecodeMemoryFileEnabled() {
    return mDecodeMemoryFileEnabled;
  }

  public FileCacheFactory getFileCacheFactory() {
    return mFileCacheFactory;
  }

  public boolean isDownsampleEnabled() {
    return mDownsampleEnabled;
  }

  public boolean isWebpSupportEnabled() {
    return mWebpSupportEnabled;
  }

  public Supplier<MemoryCacheParams> getEncodedMemoryCacheParamsSupplier() {
    return mEncodedMemoryCacheParamsSupplier;
  }

  public ExecutorSupplier getExecutorSupplier() {
    return mExecutorSupplier;
  }

  public int getForceSmallCacheThresholdBytes() {
    return mForceSmallCacheThresholdBytes;
  }

  public ImageCacheStatsTracker getImageCacheStatsTracker() {
    return mImageCacheStatsTracker;
  }

  @Nullable
  public ImageDecoder getImageDecoder() {
    return mImageDecoder;
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

  public boolean isResizeAndRotateEnabledForNetwork() {
    return mResizeAndRotateEnabledForNetwork;
  }

  public DiskCacheConfig getSmallImageDiskCacheConfig() {
    return mSmallImageDiskCacheConfig;
  }

  public static Builder newBuilder(Context context) {
    return new Builder(context);
  }

  public static class Builder {

    private AnimatedImageFactory mAnimatedImageFactory;
    private Bitmap.Config mBitmapConfig;
    private Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;
    private CacheKeyFactory mCacheKeyFactory;
    private final Context mContext;
    private boolean mDownsampleEnabled = false;
    private boolean mWebpSupportEnabled = false;
    private boolean mDecodeFileDescriptorEnabled = mDownsampleEnabled;
    private boolean mDecodeMemoryFileEnabled;
    private Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;
    private ExecutorSupplier mExecutorSupplier;
    private int mForceSmallCacheThresholdBytes = 0;
    private ImageCacheStatsTracker mImageCacheStatsTracker;
    private ImageDecoder mImageDecoder;
    private Supplier<Boolean> mIsPrefetchEnabledSupplier;
    private DiskCacheConfig mMainDiskCacheConfig;
    private MemoryTrimmableRegistry mMemoryTrimmableRegistry;
    private NetworkFetcher mNetworkFetcher;
    private PlatformBitmapFactory mPlatformBitmapFactory;
    private PoolFactory mPoolFactory;
    private ProgressiveJpegConfig mProgressiveJpegConfig;
    private Set<RequestListener> mRequestListeners;
    private boolean mResizeAndRotateEnabledForNetwork = true;
    private DiskCacheConfig mSmallImageDiskCacheConfig;
    private FileCacheFactory mFileCacheFactory;

    private Builder(Context context) {
      // Doesn't use a setter as always required.
      mContext = Preconditions.checkNotNull(context);
    }

    public Builder setAnimatedImageFactory(AnimatedImageFactory animatedImageFactory) {
      mAnimatedImageFactory = animatedImageFactory;
      return this;
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

    public Builder setCacheKeyFactory(CacheKeyFactory cacheKeyFactory) {
      mCacheKeyFactory = cacheKeyFactory;
      return this;
    }

    public Builder setDecodeFileDescriptorEnabled(boolean decodeFileDescriptorEnabled) {
      mDecodeFileDescriptorEnabled = decodeFileDescriptorEnabled;
      return this;
    }

    public Builder setDecodeMemoryFileEnabled(boolean decodeMemoryFileEnabled) {
      mDecodeMemoryFileEnabled = decodeMemoryFileEnabled;
      return this;
    }

    public Builder setFileCacheFactory(FileCacheFactory fileCacheFactory) {
      mFileCacheFactory = fileCacheFactory;
      return this;
    }

    /**
     * @deprecated use {@link Builder.setFileCacheFactory} instead
     */
    @Deprecated
    public Builder setDiskStorageFactory(DiskStorageFactory diskStorageFactory) {
      setFileCacheFactory(new DiskStorageCacheFactory(diskStorageFactory));
      return this;
    }

    public Builder setDownsampleEnabled(boolean downsampleEnabled) {
      mDownsampleEnabled = downsampleEnabled;
      return this;
    }

    public Builder setWebpSupportEnabled(boolean webpSupportEnabled) {
      mWebpSupportEnabled = webpSupportEnabled;
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

    /**
     * If this value is nonnegative, then all network-downloaded images below this size
     * will be written to the small image cache.
     *
     * <p>This will require the image pipeline to do up to two disk reads, instead of one, before
     * going out to network. Use only if this pattern makes sense for your application.
     */
    public Builder setForceSmallCacheThresholdBytes(int forceSmallCacheThresholdBytes) {
      mForceSmallCacheThresholdBytes = forceSmallCacheThresholdBytes;
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

    public Builder setResizeAndRotateEnabledForNetwork(boolean resizeAndRotateEnabledForNetwork) {
      mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
      return this;
    }

    public Builder setSmallImageDiskCacheConfig(DiskCacheConfig smallImageDiskCacheConfig) {
      mSmallImageDiskCacheConfig = smallImageDiskCacheConfig;
      return this;
    }

    public ImagePipelineConfig build() {
      return new ImagePipelineConfig(this);
    }
  }
}
