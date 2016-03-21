/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.ByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.producers.AddImageTransformMetaDataProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.BranchOnSeparateImagesProducer;
import com.facebook.imagepipeline.producers.DataFetchProducer;
import com.facebook.imagepipeline.producers.DecodeProducer;
import com.facebook.imagepipeline.producers.DiskCacheProducer;
import com.facebook.imagepipeline.producers.EncodedCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.LocalAssetFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriThumbnailFetchProducer;
import com.facebook.imagepipeline.producers.LocalExifThumbnailProducer;
import com.facebook.imagepipeline.producers.LocalFileFetchProducer;
import com.facebook.imagepipeline.producers.LocalResourceFetchProducer;
import com.facebook.imagepipeline.producers.LocalVideoThumbnailProducer;
import com.facebook.imagepipeline.producers.NetworkFetchProducer;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.NullProducer;
import com.facebook.imagepipeline.producers.PostprocessedBitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.PostprocessorProducer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.ResizeAndRotateProducer;
import com.facebook.imagepipeline.producers.SwallowResultProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.producers.ThrottlingProducer;
import com.facebook.imagepipeline.producers.ThumbnailBranchProducer;
import com.facebook.imagepipeline.producers.ThumbnailProducer;
import com.facebook.imagepipeline.producers.WebpTranscodeProducer;

public class ProducerFactory {
  // Local dependencies
  private ContentResolver mContentResolver;
  private Resources mResources;
  private AssetManager mAssetManager;

  // Decode dependencies
  private final ByteArrayPool mByteArrayPool;
  private final ImageDecoder mImageDecoder;
  private final ProgressiveJpegConfig mProgressiveJpegConfig;
  private final boolean mDownsampleEnabled;
  private final boolean mResizeAndRotateEnabledForNetwork;
  private final boolean mDecodeFileDescriptorEnabled;

  // Dependencies used by multiple steps
  private final ExecutorSupplier mExecutorSupplier;
  private final PooledByteBufferFactory mPooledByteBufferFactory;

  // Cache dependencies
  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private final MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final int mForceSmallCacheThresholdBytes;

  // Postproc dependencies
  private final PlatformBitmapFactory mPlatformBitmapFactory;

  public ProducerFactory(
      Context context,
      ByteArrayPool byteArrayPool,
      ImageDecoder imageDecoder,
      ProgressiveJpegConfig progressiveJpegConfig,
      boolean downsampleEnabled,
      boolean resizeAndRotateEnabledForNetwork,
      ExecutorSupplier executorSupplier,
      PooledByteBufferFactory pooledByteBufferFactory,
      MemoryCache<CacheKey, CloseableImage> bitmapMemoryCache,
      MemoryCache<CacheKey, PooledByteBuffer> encodedMemoryCache,
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      PlatformBitmapFactory platformBitmapFactory,
      boolean decodeFileDescriptorEnabled,
      int forceSmallCacheThresholdBytes) {
    mForceSmallCacheThresholdBytes = forceSmallCacheThresholdBytes;
    mContentResolver = context.getApplicationContext().getContentResolver();
    mResources = context.getApplicationContext().getResources();
    mAssetManager = context.getApplicationContext().getAssets();

    mByteArrayPool = byteArrayPool;
    mImageDecoder = imageDecoder;
    mProgressiveJpegConfig = progressiveJpegConfig;
    mDownsampleEnabled = downsampleEnabled;
    mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;

    mExecutorSupplier = executorSupplier;
    mPooledByteBufferFactory = pooledByteBufferFactory;

    mBitmapMemoryCache = bitmapMemoryCache;
    mEncodedMemoryCache = encodedMemoryCache;
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;

    mPlatformBitmapFactory = platformBitmapFactory;

    mDecodeFileDescriptorEnabled = decodeFileDescriptorEnabled;
  }

  public static AddImageTransformMetaDataProducer newAddImageTransformMetaDataProducer(
      Producer<EncodedImage> inputProducer) {
    return new AddImageTransformMetaDataProducer(inputProducer);
  }

  public BitmapMemoryCacheGetProducer newBitmapMemoryCacheGetProducer(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapMemoryCacheGetProducer(mBitmapMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public BitmapMemoryCacheKeyMultiplexProducer newBitmapMemoryCacheKeyMultiplexProducer(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapMemoryCacheKeyMultiplexProducer(mCacheKeyFactory, inputProducer);
  }

  public BitmapMemoryCacheProducer newBitmapMemoryCacheProducer(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapMemoryCacheProducer(mBitmapMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public static BranchOnSeparateImagesProducer newBranchOnSeparateImagesProducer(
      Producer<EncodedImage> inputProducer1,
      Producer<EncodedImage> inputProducer2) {
    return new BranchOnSeparateImagesProducer(inputProducer1, inputProducer2);
  }

  public DataFetchProducer newDataFetchProducer() {
    return new DataFetchProducer(mPooledByteBufferFactory, mDecodeFileDescriptorEnabled);
  }

  public DecodeProducer newDecodeProducer(Producer<EncodedImage> inputProducer) {
    return new DecodeProducer(
        mByteArrayPool,
        mExecutorSupplier.forDecode(),
        mImageDecoder,
        mProgressiveJpegConfig,
        mDownsampleEnabled,
        mResizeAndRotateEnabledForNetwork,
        inputProducer);
  }

  public DiskCacheProducer newDiskCacheProducer(
      Producer<EncodedImage> inputProducer) {
    return new DiskCacheProducer(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        inputProducer,
        mForceSmallCacheThresholdBytes);
  }

  public EncodedCacheKeyMultiplexProducer newEncodedCacheKeyMultiplexProducer(
      Producer<EncodedImage> inputProducer) {
    return new EncodedCacheKeyMultiplexProducer(
        mCacheKeyFactory,
        inputProducer);
  }

  public EncodedMemoryCacheProducer newEncodedMemoryCacheProducer(
      Producer<EncodedImage> inputProducer) {
    return new EncodedMemoryCacheProducer(
        mEncodedMemoryCache,
        mCacheKeyFactory,
        inputProducer);
  }

  public LocalAssetFetchProducer newLocalAssetFetchProducer() {
    return new LocalAssetFetchProducer(
        mExecutorSupplier.forLocalStorageRead(),
        mPooledByteBufferFactory,
        mAssetManager,
        mDecodeFileDescriptorEnabled);
  }

  public LocalContentUriFetchProducer newLocalContentUriFetchProducer() {
    return new LocalContentUriFetchProducer(
        mExecutorSupplier.forLocalStorageRead(),
        mPooledByteBufferFactory,
        mContentResolver,
        mDecodeFileDescriptorEnabled);
  }

    public LocalContentUriThumbnailFetchProducer newLocalContentUriThumbnailFetchProducer() {
    return new LocalContentUriThumbnailFetchProducer(
        mExecutorSupplier.forLocalStorageRead(),
        mPooledByteBufferFactory,
        mContentResolver,
        mDecodeFileDescriptorEnabled);
  }

  public LocalExifThumbnailProducer newLocalExifThumbnailProducer() {
    return new LocalExifThumbnailProducer(
        mExecutorSupplier.forLocalStorageRead(),
        mPooledByteBufferFactory,
        mContentResolver);
  }

  public ThumbnailBranchProducer newThumbnailBranchProducer(
      ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    return new ThumbnailBranchProducer(thumbnailProducers);
  }

  public LocalFileFetchProducer newLocalFileFetchProducer() {
    return new LocalFileFetchProducer(
        mExecutorSupplier.forLocalStorageRead(),
        mPooledByteBufferFactory,
        mDecodeFileDescriptorEnabled);
  }

  public LocalResourceFetchProducer newLocalResourceFetchProducer() {
    return new LocalResourceFetchProducer(
        mExecutorSupplier.forLocalStorageRead(),
        mPooledByteBufferFactory,
        mResources,
        mDecodeFileDescriptorEnabled);
  }

  public LocalVideoThumbnailProducer newLocalVideoThumbnailProducer() {
    return new LocalVideoThumbnailProducer(mExecutorSupplier.forLocalStorageRead());
  }

  public NetworkFetchProducer newNetworkFetchProducer(NetworkFetcher networkFetcher) {
    return new NetworkFetchProducer(
        mPooledByteBufferFactory,
        mByteArrayPool,
        networkFetcher);
  }

  public static <T> NullProducer<T> newNullProducer() {
    return new NullProducer<T>();
  }

  public PostprocessedBitmapMemoryCacheProducer newPostprocessorBitmapMemoryCacheProducer(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new PostprocessedBitmapMemoryCacheProducer(
        mBitmapMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public PostprocessorProducer newPostprocessorProducer(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new PostprocessorProducer(
        inputProducer, mPlatformBitmapFactory, mExecutorSupplier.forBackgroundTasks());
  }

  public ResizeAndRotateProducer newResizeAndRotateProducer(Producer<EncodedImage> inputProducer) {
    return new ResizeAndRotateProducer(
        mExecutorSupplier.forBackgroundTasks(),
        mPooledByteBufferFactory,
        inputProducer);
  }

  public static <T> SwallowResultProducer<T> newSwallowResultProducer(Producer<T> inputProducer) {
    return new SwallowResultProducer<T>(inputProducer);
  }

  public <T> ThreadHandoffProducer<T> newBackgroundThreadHandoffProducer(
      Producer<T> inputProducer, ThreadHandoffProducerQueue inputThreadHandoffProducerQueue) {
    return new ThreadHandoffProducer<T>(
        inputProducer,
        inputThreadHandoffProducerQueue);
  }

  public <T> ThrottlingProducer<T> newThrottlingProducer(
      int maxSimultaneousRequests,
      Producer<T> inputProducer) {
    return new ThrottlingProducer<T>(
        maxSimultaneousRequests,
        mExecutorSupplier.forLightweightBackgroundTasks(),
        inputProducer);
  }

  public WebpTranscodeProducer newWebpTranscodeProducer(
      Producer<EncodedImage> inputProducer) {
    return new WebpTranscodeProducer(
        mExecutorSupplier.forBackgroundTasks(),
        mPooledByteBufferFactory,
        inputProducer);
  }
}
