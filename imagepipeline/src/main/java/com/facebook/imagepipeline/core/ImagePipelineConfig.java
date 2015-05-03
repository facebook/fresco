/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry;
import com.facebook.common.util.ByteConstants;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactory;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;
import com.facebook.imagepipeline.bitmaps.GingerbreadBitmapFactory;
import com.facebook.imagepipeline.bitmaps.DalvikBitmapFactory;
import com.facebook.imagepipeline.bitmaps.ArtBitmapFactory;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.bitmaps.EmptyJpegGenerator;
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
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.HttpUrlConnectionNetworkFetcher;
import com.facebook.imagepipeline.producers.NetworkFetcher;

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
 *   ImagePipeline pipeline = factory.newPipeline();
 * </code>
 *
 * <p>This should only be done once per process.
 */
public class ImagePipelineConfig {

  // There are a lot of parameters in this class. Please follow strict alphabetical order.

  private final Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Context mContext;
  private final Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;
  private final ExecutorSupplier mExecutorSupplier;
  private final ImageCacheStatsTracker mImageCacheStatsTracker;
  private final AnimatedDrawableUtil mAnimatedDrawableUtil;
  private final AnimatedImageFactory mAnimatedImageFactory;
  private final ImageDecoder mImageDecoder;
  private final Supplier<Boolean> mIsPrefetchEnabledSupplier;
  private final DiskCacheConfig mMainDiskCacheConfig;
  private final MemoryTrimmableRegistry mMemoryTrimmableRegistry;
  private final NetworkFetcher mNetworkFetcher;
  private final PoolFactory mPoolFactory;
  private final ProgressiveJpegConfig mProgressiveJpegConfig;
  private final Set<RequestListener> mRequestListeners;
  private final boolean mResizeAndRotateEnabledForNetwork;
  private final DiskCacheConfig mSmallImageDiskCacheConfig;
  private final PlatformBitmapFactory mPlatformBitmapFactory;

  private ImagePipelineConfig(Builder builder) {
    mBitmapMemoryCacheParamsSupplier =
        builder.mBitmapMemoryCacheParamsSupplier == null ?
            new DefaultBitmapMemoryCacheParamsSupplier(
                (ActivityManager) builder.mContext.getSystemService(Context.ACTIVITY_SERVICE)) :
            builder.mBitmapMemoryCacheParamsSupplier;
    mCacheKeyFactory =
        builder.mCacheKeyFactory == null ?
            DefaultCacheKeyFactory.getInstance() :
            builder.mCacheKeyFactory;
    mContext = Preconditions.checkNotNull(builder.mContext);
    mEncodedMemoryCacheParamsSupplier =
        builder.mEncodedMemoryCacheParamsSupplier == null ?
            new DefaultEncodedMemoryCacheParamsSupplier() :
            builder.mEncodedMemoryCacheParamsSupplier;
    mExecutorSupplier =
        builder.mExecutorSupplier == null ?
            new DefaultExecutorSupplier() :
            builder.mExecutorSupplier;
    mImageCacheStatsTracker =
        builder.mImageCacheStatsTracker == null ?
            NoOpImageCacheStatsTracker.getInstance() :
            builder.mImageCacheStatsTracker;
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

    mAnimatedDrawableUtil = new AnimatedDrawableUtil();
    AnimatedDrawableBackendProvider animatedDrawableBackendProvider =
        new AnimatedDrawableBackendProvider() {
      @Override
      public AnimatedDrawableBackend get(AnimatedImageResult imageResult, Rect bounds) {
        return new AnimatedDrawableBackendImpl(mAnimatedDrawableUtil, imageResult, bounds);
      }
    };
    GingerbreadBitmapFactory factoryGingerbread = new GingerbreadBitmapFactory();
    DalvikBitmapFactory factoryICS = new DalvikBitmapFactory(
        new EmptyJpegGenerator(mPoolFactory.getPooledByteBufferFactory()),
        mPoolFactory.getSharedByteArray());
    ArtBitmapFactory factoryLollipop =
        new ArtBitmapFactory(mPoolFactory.getBitmapPool());
    mPlatformBitmapFactory =
        new PlatformBitmapFactory(
            factoryGingerbread,
            factoryICS,
            factoryLollipop);

    mAnimatedImageFactory = builder.mAnimatedImageFactory == null ?
        new AnimatedImageFactory(animatedDrawableBackendProvider, mPlatformBitmapFactory) :
        builder.mAnimatedImageFactory;

    mImageDecoder =
        builder.mImageDecoder == null ?
            new ImageDecoder(mAnimatedImageFactory, mPlatformBitmapFactory) :
            builder.mImageDecoder;
    mNetworkFetcher =
        builder.mNetworkFetcher == null ?
            new HttpUrlConnectionNetworkFetcher() :
            builder.mNetworkFetcher;
  }

  private static DiskCacheConfig getDefaultMainDiskCacheConfig(final Context context) {
    return DiskCacheConfig.newBuilder()
        .setBaseDirectoryPathSupplier(
            new Supplier<File>() {
              @Override
              public File get() {
                return context.getApplicationContext().getCacheDir();
              }
            })
        .setBaseDirectoryName("image_cache")
        .setMaxCacheSize(40 * ByteConstants.MB)
        .setMaxCacheSizeOnLowDiskSpace(10 * ByteConstants.MB)
        .setMaxCacheSizeOnVeryLowDiskSpace(2 * ByteConstants.MB)
        .build();
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

  public Supplier<MemoryCacheParams> getEncodedMemoryCacheParamsSupplier() {
    return mEncodedMemoryCacheParamsSupplier;
  }

  public ExecutorSupplier getExecutorSupplier() {
    return mExecutorSupplier;
  }

  public ImageCacheStatsTracker getImageCacheStatsTracker() {
    return mImageCacheStatsTracker;
  }

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

  public PlatformBitmapFactory getPlatformBitmapFactory() {
    return mPlatformBitmapFactory;
  }

  public static Builder newBuilder(Context context) {
    return new Builder(context);
  }

  public static class Builder {

    private Supplier<MemoryCacheParams> mBitmapMemoryCacheParamsSupplier;
    private CacheKeyFactory mCacheKeyFactory;
    private final Context mContext;
    private Supplier<MemoryCacheParams> mEncodedMemoryCacheParamsSupplier;
    private ExecutorSupplier mExecutorSupplier;
    private ImageCacheStatsTracker mImageCacheStatsTracker;
    private ImageDecoder mImageDecoder;
    private Supplier<Boolean> mIsPrefetchEnabledSupplier;
    private DiskCacheConfig mMainDiskCacheConfig;
    private MemoryTrimmableRegistry mMemoryTrimmableRegistry;
    private NetworkFetcher mNetworkFetcher;
    private PoolFactory mPoolFactory;
    private ProgressiveJpegConfig mProgressiveJpegConfig;
    private Set<RequestListener> mRequestListeners;
    private boolean mResizeAndRotateEnabledForNetwork = true;
    private DiskCacheConfig mSmallImageDiskCacheConfig;
    private AnimatedImageFactory mAnimatedImageFactory;

    private Builder(Context context) {
      // Doesn't use a setter as always required.
      mContext = Preconditions.checkNotNull(context);
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

    public Builder setAnimatedImageFactory(AnimatedImageFactory animatedImageFactory) {
      mAnimatedImageFactory = animatedImageFactory;
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
