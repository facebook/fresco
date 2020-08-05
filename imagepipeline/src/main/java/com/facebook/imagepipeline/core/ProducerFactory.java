/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.BoundedLinkedHashSet;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.AddImageTransformMetaDataProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.BitmapPrepareProducer;
import com.facebook.imagepipeline.producers.BitmapProbeProducer;
import com.facebook.imagepipeline.producers.BranchOnSeparateImagesProducer;
import com.facebook.imagepipeline.producers.DataFetchProducer;
import com.facebook.imagepipeline.producers.DecodeProducer;
import com.facebook.imagepipeline.producers.DiskCacheReadProducer;
import com.facebook.imagepipeline.producers.DiskCacheWriteProducer;
import com.facebook.imagepipeline.producers.EncodedCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.EncodedProbeProducer;
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
import com.facebook.imagepipeline.producers.PartialDiskCacheProducer;
import com.facebook.imagepipeline.producers.PostprocessedBitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.PostprocessorProducer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.QualifiedResourceFetchProducer;
import com.facebook.imagepipeline.producers.ResizeAndRotateProducer;
import com.facebook.imagepipeline.producers.SwallowResultProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.producers.ThrottlingProducer;
import com.facebook.imagepipeline.producers.ThumbnailBranchProducer;
import com.facebook.imagepipeline.producers.ThumbnailProducer;
import com.facebook.imagepipeline.producers.WebpTranscodeProducer;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;

public class ProducerFactory {

  private static final int MAX_SIMULTANEOUS_REQUESTS = 5;

  // Local dependencies
  protected ContentResolver mContentResolver;
  protected Resources mResources;
  protected AssetManager mAssetManager;

  // Decode dependencies
  protected final ByteArrayPool mByteArrayPool;
  protected final ImageDecoder mImageDecoder;
  protected final ProgressiveJpegConfig mProgressiveJpegConfig;
  protected final boolean mDownsampleEnabled;
  protected final boolean mResizeAndRotateEnabledForNetwork;
  protected final boolean mDecodeCancellationEnabled;

  // Dependencies used by multiple steps
  protected final ExecutorSupplier mExecutorSupplier;
  protected final PooledByteBufferFactory mPooledByteBufferFactory;

  // Cache dependencies
  protected final BufferedDiskCache mDefaultBufferedDiskCache;
  protected final BufferedDiskCache mSmallImageBufferedDiskCache;
  protected final MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  protected final MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
  protected final CacheKeyFactory mCacheKeyFactory;
  protected final BoundedLinkedHashSet<CacheKey> mEncodedMemoryCacheHistory;
  protected final BoundedLinkedHashSet<CacheKey> mDiskCacheHistory;

  // Postproc dependencies
  protected final PlatformBitmapFactory mPlatformBitmapFactory;

  // BitmapPrepare dependencies
  protected final int mBitmapPrepareToDrawMinSizeBytes;
  protected final int mBitmapPrepareToDrawMaxSizeBytes;
  protected boolean mBitmapPrepareToDrawForPrefetch;

  // Core factory dependencies
  protected final CloseableReferenceFactory mCloseableReferenceFactory;

  protected final int mMaxBitmapSize;

  protected final boolean mKeepCancelledFetchAsLowPriority;

  public ProducerFactory(
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
    mContentResolver = context.getApplicationContext().getContentResolver();
    mResources = context.getApplicationContext().getResources();
    mAssetManager = context.getApplicationContext().getAssets();

    mByteArrayPool = byteArrayPool;
    mImageDecoder = imageDecoder;
    mProgressiveJpegConfig = progressiveJpegConfig;
    mDownsampleEnabled = downsampleEnabled;
    mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
    mDecodeCancellationEnabled = decodeCancellationEnabled;

    mExecutorSupplier = executorSupplier;
    mPooledByteBufferFactory = pooledByteBufferFactory;

    mBitmapMemoryCache = bitmapMemoryCache;
    mEncodedMemoryCache = encodedMemoryCache;
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mPlatformBitmapFactory = platformBitmapFactory;
    mEncodedMemoryCacheHistory = new BoundedLinkedHashSet<>(trackedKeysSize);
    mDiskCacheHistory = new BoundedLinkedHashSet<>(trackedKeysSize);

    mBitmapPrepareToDrawMinSizeBytes = bitmapPrepareToDrawMinSizeBytes;
    mBitmapPrepareToDrawMaxSizeBytes = bitmapPrepareToDrawMaxSizeBytes;
    mBitmapPrepareToDrawForPrefetch = bitmapPrepareToDrawForPrefetch;

    mMaxBitmapSize = maxBitmapSize;
    mCloseableReferenceFactory = closeableReferenceFactory;

    mKeepCancelledFetchAsLowPriority = keepCancelledFetchAsLowPriority;
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
      Producer<EncodedImage> inputProducer1, Producer<EncodedImage> inputProducer2) {
    return new BranchOnSeparateImagesProducer(inputProducer1, inputProducer2);
  }

  public DataFetchProducer newDataFetchProducer() {
    return new DataFetchProducer(mPooledByteBufferFactory);
  }

  public DecodeProducer newDecodeProducer(Producer<EncodedImage> inputProducer) {
    return new DecodeProducer(
        mByteArrayPool,
        mExecutorSupplier.forDecode(),
        mImageDecoder,
        mProgressiveJpegConfig,
        mDownsampleEnabled,
        mResizeAndRotateEnabledForNetwork,
        mDecodeCancellationEnabled,
        inputProducer,
        mMaxBitmapSize,
        mCloseableReferenceFactory,
        null,
        Suppliers.BOOLEAN_FALSE);
  }

  public DiskCacheReadProducer newDiskCacheReadProducer(Producer<EncodedImage> inputProducer) {
    return new DiskCacheReadProducer(
        mDefaultBufferedDiskCache, mSmallImageBufferedDiskCache, mCacheKeyFactory, inputProducer);
  }

  public DiskCacheWriteProducer newDiskCacheWriteProducer(Producer<EncodedImage> inputProducer) {
    return new DiskCacheWriteProducer(
        mDefaultBufferedDiskCache, mSmallImageBufferedDiskCache, mCacheKeyFactory, inputProducer);
  }

  public PartialDiskCacheProducer newPartialDiskCacheProducer(
      Producer<EncodedImage> inputProducer) {
    return new PartialDiskCacheProducer(
        mDefaultBufferedDiskCache,
        mCacheKeyFactory,
        mPooledByteBufferFactory,
        mByteArrayPool,
        inputProducer);
  }

  public EncodedCacheKeyMultiplexProducer newEncodedCacheKeyMultiplexProducer(
      Producer<EncodedImage> inputProducer) {
    return new EncodedCacheKeyMultiplexProducer(
        mCacheKeyFactory, mKeepCancelledFetchAsLowPriority, inputProducer);
  }

  public BitmapProbeProducer newBitmapProbeProducer(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapProbeProducer(
        mEncodedMemoryCache,
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mEncodedMemoryCacheHistory,
        mDiskCacheHistory,
        inputProducer);
  }

  public EncodedProbeProducer newEncodedProbeProducer(Producer<EncodedImage> inputProducer) {
    return new EncodedProbeProducer(
        mDefaultBufferedDiskCache,
        mSmallImageBufferedDiskCache,
        mCacheKeyFactory,
        mEncodedMemoryCacheHistory,
        mDiskCacheHistory,
        inputProducer);
  }

  public EncodedMemoryCacheProducer newEncodedMemoryCacheProducer(
      Producer<EncodedImage> inputProducer) {
    return new EncodedMemoryCacheProducer(mEncodedMemoryCache, mCacheKeyFactory, inputProducer);
  }

  public LocalAssetFetchProducer newLocalAssetFetchProducer() {
    return new LocalAssetFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mAssetManager);
  }

  public LocalContentUriFetchProducer newLocalContentUriFetchProducer() {
    return new LocalContentUriFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mContentResolver);
  }

  public LocalContentUriThumbnailFetchProducer newLocalContentUriThumbnailFetchProducer() {
    return new LocalContentUriThumbnailFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mContentResolver);
  }

  public LocalExifThumbnailProducer newLocalExifThumbnailProducer() {
    return new LocalExifThumbnailProducer(
        mExecutorSupplier.forThumbnailProducer(), mPooledByteBufferFactory, mContentResolver);
  }

  public ThumbnailBranchProducer newThumbnailBranchProducer(
      ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    return new ThumbnailBranchProducer(thumbnailProducers);
  }

  public LocalFileFetchProducer newLocalFileFetchProducer() {
    return new LocalFileFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory);
  }

  public QualifiedResourceFetchProducer newQualifiedResourceFetchProducer() {
    return new QualifiedResourceFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mContentResolver);
  }

  public LocalResourceFetchProducer newLocalResourceFetchProducer() {
    return new LocalResourceFetchProducer(
        mExecutorSupplier.forLocalStorageRead(), mPooledByteBufferFactory, mResources);
  }

  public LocalVideoThumbnailProducer newLocalVideoThumbnailProducer() {
    return new LocalVideoThumbnailProducer(
        mExecutorSupplier.forLocalStorageRead(), mContentResolver);
  }

  public Producer<EncodedImage> newNetworkFetchProducer(NetworkFetcher networkFetcher) {
    return new NetworkFetchProducer(mPooledByteBufferFactory, mByteArrayPool, networkFetcher);
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

  public ResizeAndRotateProducer newResizeAndRotateProducer(
      Producer<EncodedImage> inputProducer,
      final boolean isResizingEnabled,
      ImageTranscoderFactory imageTranscoderFactory) {
    return new ResizeAndRotateProducer(
        mExecutorSupplier.forBackgroundTasks(),
        mPooledByteBufferFactory,
        inputProducer,
        isResizingEnabled,
        imageTranscoderFactory);
  }

  public static <T> SwallowResultProducer<T> newSwallowResultProducer(Producer<T> inputProducer) {
    return new SwallowResultProducer<T>(inputProducer);
  }

  public <T> Producer<T> newBackgroundThreadHandoffProducer(
      Producer<T> inputProducer, ThreadHandoffProducerQueue inputThreadHandoffProducerQueue) {
    return new ThreadHandoffProducer<T>(inputProducer, inputThreadHandoffProducerQueue);
  }

  public <T> ThrottlingProducer<T> newThrottlingProducer(Producer<T> inputProducer) {
    return new ThrottlingProducer<T>(
        MAX_SIMULTANEOUS_REQUESTS,
        mExecutorSupplier.forLightweightBackgroundTasks(),
        inputProducer);
  }

  public WebpTranscodeProducer newWebpTranscodeProducer(Producer<EncodedImage> inputProducer) {
    return new WebpTranscodeProducer(
        mExecutorSupplier.forBackgroundTasks(), mPooledByteBufferFactory, inputProducer);
  }

  public BitmapPrepareProducer newBitmapPrepareProducer(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    return new BitmapPrepareProducer(
        inputProducer,
        mBitmapPrepareToDrawMinSizeBytes,
        mBitmapPrepareToDrawMaxSizeBytes,
        mBitmapPrepareToDrawForPrefetch);
  }
}
