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
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheFactory;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.CountingMemoryCache;
import com.facebook.imagepipeline.cache.ImageCacheStatsTracker;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoderConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestListener2;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Set;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface ImagePipelineConfigInterface {

  Bitmap.Config getBitmapConfig();

  Supplier<MemoryCacheParams> getBitmapMemoryCacheParamsSupplier();

  MemoryCache.CacheTrimStrategy getBitmapMemoryCacheTrimStrategy();

  @Nullable
  CountingMemoryCache.EntryStateObserver<CacheKey> getBitmapMemoryCacheEntryStateObserver();

  CacheKeyFactory getCacheKeyFactory();

  Context getContext();

  FileCacheFactory getFileCacheFactory();

  boolean isDownsampleEnabled();

  boolean isDiskCacheEnabled();

  Supplier<MemoryCacheParams> getEncodedMemoryCacheParamsSupplier();

  ExecutorSupplier getExecutorSupplier();

  @Nullable
  SerialExecutorService getExecutorServiceForAnimatedImages();

  ImageCacheStatsTracker getImageCacheStatsTracker();

  @Nullable
  ImageDecoder getImageDecoder();

  @Nullable
  ImageTranscoderFactory getImageTranscoderFactory();

  @Nullable
  @ImageTranscoderType
  Integer getImageTranscoderType();

  Supplier<Boolean> getIsPrefetchEnabledSupplier();

  DiskCacheConfig getMainDiskCacheConfig();

  MemoryTrimmableRegistry getMemoryTrimmableRegistry();

  @MemoryChunkType
  int getMemoryChunkType();

  NetworkFetcher getNetworkFetcher();

  @Nullable
  PlatformBitmapFactory getPlatformBitmapFactory();

  PoolFactory getPoolFactory();

  ProgressiveJpegConfig getProgressiveJpegConfig();

  Set<RequestListener> getRequestListeners();

  Set<RequestListener2> getRequestListener2s();

  boolean isResizeAndRotateEnabledForNetwork();

  DiskCacheConfig getSmallImageDiskCacheConfig();

  @Nullable
  ImageDecoderConfig getImageDecoderConfig();

  @Nullable
  CallerContextVerifier getCallerContextVerifier();

  ImagePipelineExperiments getExperiments();

  CloseableReferenceLeakTracker getCloseableReferenceLeakTracker();

  @Nullable
  MemoryCache<CacheKey, CloseableImage> getBitmapCacheOverride();

  @Nullable
  MemoryCache<CacheKey, PooledByteBuffer> getEncodedMemoryCacheOverride();

  BitmapMemoryCacheFactory getBitmapMemoryCacheFactory();
}
