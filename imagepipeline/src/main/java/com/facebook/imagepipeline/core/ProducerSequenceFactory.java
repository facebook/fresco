/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_DATA;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_ASSET;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_CONTENT;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_NETWORK;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE;

import android.content.ContentResolver;
import android.net.Uri;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.DecodeProducer;
import com.facebook.imagepipeline.producers.DiskCacheReadProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.EncodedProbeProducer;
import com.facebook.imagepipeline.producers.LocalAssetFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriFetchProducer;
import com.facebook.imagepipeline.producers.LocalFileFetchProducer;
import com.facebook.imagepipeline.producers.LocalResourceFetchProducer;
import com.facebook.imagepipeline.producers.LocalVideoThumbnailProducer;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.PostprocessedBitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.PostprocessorProducer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.QualifiedResourceFetchProducer;
import com.facebook.imagepipeline.producers.RemoveImageTransformMetaDataProducer;
import com.facebook.imagepipeline.producers.SwallowResultProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.producers.ThrottlingProducer;
import com.facebook.imagepipeline.producers.ThumbnailBranchProducer;
import com.facebook.imagepipeline.producers.ThumbnailProducer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import java.util.HashMap;
import java.util.Map;

public class ProducerSequenceFactory {

  private final ContentResolver mContentResolver;
  private final ProducerFactory mProducerFactory;
  private final NetworkFetcher mNetworkFetcher;
  private final boolean mResizeAndRotateEnabledForNetwork;
  private final boolean mWebpSupportEnabled;
  private final boolean mPartialImageCachingEnabled;
  private final ThreadHandoffProducerQueue mThreadHandoffProducerQueue;
  private final boolean mDownsampleEnabled;
  private final boolean mUseBitmapPrepareToDraw;
  private final boolean mDiskCacheEnabled;
  private final ImageTranscoderFactory mImageTranscoderFactory;
  private final boolean mIsEncodedMemoryCacheProbingEnabled;
  private final boolean mIsDiskCacheProbingEnabled;

  // Saved sequences
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mNetworkFetchSequence;
  @VisibleForTesting Producer<EncodedImage> mBackgroundLocalFileFetchToEncodedMemorySequence;
  @VisibleForTesting Producer<EncodedImage> mBackgroundLocalContentUriFetchToEncodedMemorySequence;
  @VisibleForTesting Producer<EncodedImage> mBackgroundNetworkFetchToEncodedMemorySequence;

  @VisibleForTesting
  Producer<CloseableReference<PooledByteBuffer>> mLocalFileEncodedImageProducerSequence;

  @VisibleForTesting
  Producer<CloseableReference<PooledByteBuffer>> mLocalContentUriEncodedImageProducerSequence;

  @VisibleForTesting
  Producer<CloseableReference<PooledByteBuffer>> mNetworkEncodedImageProducerSequence;

  @VisibleForTesting Producer<Void> mLocalFileFetchToEncodedMemoryPrefetchSequence;
  @VisibleForTesting Producer<Void> mNetworkFetchToEncodedMemoryPrefetchSequence;
  private Producer<EncodedImage> mCommonNetworkFetchToEncodedMemorySequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalImageFileFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalVideoFileFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalContentUriFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalResourceFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalAssetFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mDataFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mQualifiedResourceFetchSequence;

  @VisibleForTesting
  Map<Producer<CloseableReference<CloseableImage>>, Producer<CloseableReference<CloseableImage>>>
      mPostprocessorSequences;

  @VisibleForTesting
  Map<Producer<CloseableReference<CloseableImage>>, Producer<Void>>
      mCloseableImagePrefetchSequences;

  @VisibleForTesting
  Map<Producer<CloseableReference<CloseableImage>>, Producer<CloseableReference<CloseableImage>>>
      mBitmapPrepareSequences;

  public ProducerSequenceFactory(
      ContentResolver contentResolver,
      ProducerFactory producerFactory,
      NetworkFetcher networkFetcher,
      boolean resizeAndRotateEnabledForNetwork,
      boolean webpSupportEnabled,
      ThreadHandoffProducerQueue threadHandoffProducerQueue,
      boolean downSampleEnabled,
      boolean useBitmapPrepareToDraw,
      boolean partialImageCachingEnabled,
      boolean diskCacheEnabled,
      ImageTranscoderFactory imageTranscoderFactory,
      boolean isEncodedMemoryCacheProbingEnabled,
      boolean isDiskCacheProbingEnabled) {
    mContentResolver = contentResolver;
    mProducerFactory = producerFactory;
    mNetworkFetcher = networkFetcher;
    mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
    mWebpSupportEnabled = webpSupportEnabled;
    mPostprocessorSequences = new HashMap<>();
    mCloseableImagePrefetchSequences = new HashMap<>();
    mBitmapPrepareSequences = new HashMap<>();
    mThreadHandoffProducerQueue = threadHandoffProducerQueue;
    mDownsampleEnabled = downSampleEnabled;
    mUseBitmapPrepareToDraw = useBitmapPrepareToDraw;
    mPartialImageCachingEnabled = partialImageCachingEnabled;
    mDiskCacheEnabled = diskCacheEnabled;
    mImageTranscoderFactory = imageTranscoderFactory;
    mIsEncodedMemoryCacheProbingEnabled = isEncodedMemoryCacheProbingEnabled;
    mIsDiskCacheProbingEnabled = isDiskCacheProbingEnabled;
  }

  /**
   * Returns a sequence that can be used for a request for an encoded image from either network or
   * local files.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<CloseableReference<PooledByteBuffer>> getEncodedImageProducerSequence(
      ImageRequest imageRequest) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ProducerSequenceFactory#getEncodedImageProducerSequence");
      }
      validateEncodedImageRequest(imageRequest);
      final Uri uri = imageRequest.getSourceUri();

      switch (imageRequest.getSourceUriType()) {
        case SOURCE_TYPE_NETWORK:
          return getNetworkFetchEncodedImageProducerSequence();
        case SOURCE_TYPE_LOCAL_VIDEO_FILE:
        case SOURCE_TYPE_LOCAL_IMAGE_FILE:
          return getLocalFileFetchEncodedImageProducerSequence();
        case SOURCE_TYPE_LOCAL_CONTENT:
          return getLocalContentUriFetchEncodedImageProducerSequence();
        default:
          throw new IllegalArgumentException(
              "Unsupported uri scheme for encoded image fetch! Uri is: "
                  + getShortenedUriString(uri));
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /** Returns a sequence that can be used for a request for an encoded image from network. */
  public Producer<CloseableReference<PooledByteBuffer>>
      getNetworkFetchEncodedImageProducerSequence() {
    synchronized (this) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getNetworkFetchEncodedImageProducerSequence");
      }
      if (mNetworkEncodedImageProducerSequence == null) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection(
              "ProducerSequenceFactory#getNetworkFetchEncodedImageProducerSequence:init");
        }
        mNetworkEncodedImageProducerSequence =
            new RemoveImageTransformMetaDataProducer(
                getBackgroundNetworkFetchToEncodedMemorySequence());
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    return mNetworkEncodedImageProducerSequence;
  }

  /** Returns a sequence that can be used for a request for an encoded image from a local file. */
  public Producer<CloseableReference<PooledByteBuffer>>
      getLocalFileFetchEncodedImageProducerSequence() {
    synchronized (this) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getLocalFileFetchEncodedImageProducerSequence");
      }
      if (mLocalFileEncodedImageProducerSequence == null) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection(
              "ProducerSequenceFactory#getLocalFileFetchEncodedImageProducerSequence:init");
        }
        mLocalFileEncodedImageProducerSequence =
            new RemoveImageTransformMetaDataProducer(
                getBackgroundLocalFileFetchToEncodeMemorySequence());
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    return mLocalFileEncodedImageProducerSequence;
  }

  /**
   * Returns a sequence that can be used for a request for an encoded image from a local content
   * uri.
   */
  public Producer<CloseableReference<PooledByteBuffer>>
      getLocalContentUriFetchEncodedImageProducerSequence() {
    synchronized (this) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getLocalContentUriFetchEncodedImageProducerSequence");
      }
      if (mLocalContentUriEncodedImageProducerSequence == null) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection(
              "ProducerSequenceFactory#getLocalContentUriFetchEncodedImageProducerSequence:init");
        }
        mLocalContentUriEncodedImageProducerSequence =
            new RemoveImageTransformMetaDataProducer(
                getBackgroundLocalContentUriFetchToEncodeMemorySequence());
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.endSection();
        }
      }
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    return mLocalContentUriEncodedImageProducerSequence;
  }

  /**
   * Returns a sequence that can be used for a prefetch request for an encoded image.
   *
   * <p>Guaranteed to return the same sequence as {@code getEncodedImageProducerSequence(request)},
   * except that it is pre-pended with a {@link SwallowResultProducer}.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<Void> getEncodedImagePrefetchProducerSequence(ImageRequest imageRequest) {
    validateEncodedImageRequest(imageRequest);

    switch (imageRequest.getSourceUriType()) {
      case SOURCE_TYPE_NETWORK:
        return getNetworkFetchToEncodedMemoryPrefetchSequence();
      case SOURCE_TYPE_LOCAL_VIDEO_FILE:
      case SOURCE_TYPE_LOCAL_IMAGE_FILE:
        return getLocalFileFetchToEncodedMemoryPrefetchSequence();
      default:
        final Uri uri = imageRequest.getSourceUri();
        throw new IllegalArgumentException(
            "Unsupported uri scheme for encoded image fetch! Uri is: "
                + getShortenedUriString(uri));
    }
  }

  private static void validateEncodedImageRequest(ImageRequest imageRequest) {
    Preconditions.checkNotNull(imageRequest);
    Preconditions.checkArgument(
        imageRequest.getLowestPermittedRequestLevel().getValue()
            <= ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE.getValue());
  }

  /**
   * Returns a sequence that can be used for a request for a decoded image.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<CloseableReference<CloseableImage>> getDecodedImageProducerSequence(
      ImageRequest imageRequest) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#getDecodedImageProducerSequence");
    }
    Producer<CloseableReference<CloseableImage>> pipelineSequence =
        getBasicDecodedImageSequence(imageRequest);

    if (imageRequest.getPostprocessor() != null) {
      pipelineSequence = getPostprocessorSequence(pipelineSequence);
    }

    if (mUseBitmapPrepareToDraw) {
      pipelineSequence = getBitmapPrepareSequence(pipelineSequence);
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return pipelineSequence;
  }

  /**
   * Returns a sequence that can be used for a prefetch request for a decoded image.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<Void> getDecodedImagePrefetchProducerSequence(ImageRequest imageRequest) {
    Producer<CloseableReference<CloseableImage>> inputProducer =
        getBasicDecodedImageSequence(imageRequest);

    if (mUseBitmapPrepareToDraw) {
      inputProducer = getBitmapPrepareSequence(inputProducer);
    }

    return getDecodedImagePrefetchSequence(inputProducer);
  }

  private Producer<CloseableReference<CloseableImage>> getBasicDecodedImageSequence(
      ImageRequest imageRequest) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ProducerSequenceFactory#getBasicDecodedImageSequence");
      }
      Preconditions.checkNotNull(imageRequest);

      Uri uri = imageRequest.getSourceUri();
      Preconditions.checkNotNull(uri, "Uri is null.");

      switch (imageRequest.getSourceUriType()) {
        case SOURCE_TYPE_NETWORK:
          return getNetworkFetchSequence();
        case SOURCE_TYPE_LOCAL_VIDEO_FILE:
          return getLocalVideoFileFetchSequence();
        case SOURCE_TYPE_LOCAL_IMAGE_FILE:
          return getLocalImageFileFetchSequence();
        case SOURCE_TYPE_LOCAL_CONTENT:
          if (MediaUtils.isVideo(mContentResolver.getType(uri))) {
            return getLocalVideoFileFetchSequence();
          }
          return getLocalContentUriFetchSequence();
        case SOURCE_TYPE_LOCAL_ASSET:
          return getLocalAssetFetchSequence();
        case SOURCE_TYPE_LOCAL_RESOURCE:
          return getLocalResourceFetchSequence();
        case SOURCE_TYPE_QUALIFIED_RESOURCE:
          return getQualifiedResourceFetchSequence();
        case SOURCE_TYPE_DATA:
          return getDataFetchSequence();
        default:
          throw new IllegalArgumentException(
              "Unsupported uri scheme! Uri is: " + getShortenedUriString(uri));
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * swallow result if prefetch -> bitmap cache get -> background thread hand-off -> multiplex ->
   * bitmap cache -> decode -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * network fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>> getNetworkFetchSequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#getNetworkFetchSequence");
    }
    if (mNetworkFetchSequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("ProducerSequenceFactory#getNetworkFetchSequence:init");
      }
      mNetworkFetchSequence =
          newBitmapCacheGetToDecodeSequence(getCommonNetworkFetchToEncodedMemorySequence());
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mNetworkFetchSequence;
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * network fetch.
   */
  private synchronized Producer<EncodedImage> getBackgroundNetworkFetchToEncodedMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getBackgroundNetworkFetchToEncodedMemorySequence");
    }
    if (mBackgroundNetworkFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getBackgroundNetworkFetchToEncodedMemorySequence:init");
      }
      // Use hand-off producer to ensure that we don't do any unnecessary work on the UI thread.
      mBackgroundNetworkFetchToEncodedMemorySequence =
          mProducerFactory.newBackgroundThreadHandoffProducer(
              getCommonNetworkFetchToEncodedMemorySequence(), mThreadHandoffProducerQueue);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mBackgroundNetworkFetchToEncodedMemorySequence;
  }

  /**
   * swallow-result -> background-thread hand-off -> multiplex -> encoded cache -> disk cache ->
   * (webp transcode) -> network fetch.
   */
  private synchronized Producer<Void> getNetworkFetchToEncodedMemoryPrefetchSequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getNetworkFetchToEncodedMemoryPrefetchSequence");
    }
    if (mNetworkFetchToEncodedMemoryPrefetchSequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getNetworkFetchToEncodedMemoryPrefetchSequence:init");
      }
      mNetworkFetchToEncodedMemoryPrefetchSequence =
          ProducerFactory.newSwallowResultProducer(
              getBackgroundNetworkFetchToEncodedMemorySequence());
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mNetworkFetchToEncodedMemoryPrefetchSequence;
  }

  /** multiplex -> encoded cache -> disk cache -> (webp transcode) -> network fetch. */
  private synchronized Producer<EncodedImage> getCommonNetworkFetchToEncodedMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getCommonNetworkFetchToEncodedMemorySequence");
    }
    if (mCommonNetworkFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getCommonNetworkFetchToEncodedMemorySequence:init");
      }
      Producer<EncodedImage> inputProducer =
          newEncodedCacheMultiplexToTranscodeSequence(
              mProducerFactory.newNetworkFetchProducer(mNetworkFetcher));
      mCommonNetworkFetchToEncodedMemorySequence =
          ProducerFactory.newAddImageTransformMetaDataProducer(inputProducer);

      mCommonNetworkFetchToEncodedMemorySequence =
          mProducerFactory.newResizeAndRotateProducer(
              mCommonNetworkFetchToEncodedMemorySequence,
              mResizeAndRotateEnabledForNetwork && !mDownsampleEnabled,
              mImageTranscoderFactory);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mCommonNetworkFetchToEncodedMemorySequence;
  }

  /**
   * swallow-result -> background-thread hand-off -> multiplex -> encoded cache -> disk cache ->
   * (webp transcode) -> local file fetch.
   */
  private synchronized Producer<Void> getLocalFileFetchToEncodedMemoryPrefetchSequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getLocalFileFetchToEncodedMemoryPrefetchSequence");
    }
    if (mLocalFileFetchToEncodedMemoryPrefetchSequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getLocalFileFetchToEncodedMemoryPrefetchSequence:init");
      }
      mLocalFileFetchToEncodedMemoryPrefetchSequence =
          ProducerFactory.newSwallowResultProducer(
              getBackgroundLocalFileFetchToEncodeMemorySequence());
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mLocalFileFetchToEncodedMemoryPrefetchSequence;
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * local file fetch
   */
  private synchronized Producer<EncodedImage> getBackgroundLocalFileFetchToEncodeMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getBackgroundLocalFileFetchToEncodeMemorySequence");
    }
    if (mBackgroundLocalFileFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getBackgroundLocalFileFetchToEncodeMemorySequence:init");
      }
      final LocalFileFetchProducer localFileFetchProducer =
          mProducerFactory.newLocalFileFetchProducer();

      final Producer<EncodedImage> toEncodedMultiplexProducer =
          newEncodedCacheMultiplexToTranscodeSequence(localFileFetchProducer);

      mBackgroundLocalFileFetchToEncodedMemorySequence =
          mProducerFactory.newBackgroundThreadHandoffProducer(
              toEncodedMultiplexProducer, mThreadHandoffProducerQueue);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mBackgroundLocalFileFetchToEncodedMemorySequence;
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * local content resolver fetch
   */
  private synchronized Producer<EncodedImage>
      getBackgroundLocalContentUriFetchToEncodeMemorySequence() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection(
          "ProducerSequenceFactory#getBackgroundLocalContentUriFetchToEncodeMemorySequence");
    }
    if (mBackgroundLocalContentUriFetchToEncodedMemorySequence == null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection(
            "ProducerSequenceFactory#getBackgroundLocalContentUriFetchToEncodeMemorySequence:init");
      }
      final LocalContentUriFetchProducer localFileFetchProducer =
          mProducerFactory.newLocalContentUriFetchProducer();

      final Producer<EncodedImage> toEncodedMultiplexProducer =
          newEncodedCacheMultiplexToTranscodeSequence(localFileFetchProducer);

      mBackgroundLocalContentUriFetchToEncodedMemorySequence =
          mProducerFactory.newBackgroundThreadHandoffProducer(
              toEncodedMultiplexProducer, mThreadHandoffProducerQueue);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return mBackgroundLocalContentUriFetchToEncodedMemorySequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * file fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getLocalImageFileFetchSequence() {
    if (mLocalImageFileFetchSequence == null) {
      LocalFileFetchProducer localFileFetchProducer = mProducerFactory.newLocalFileFetchProducer();
      mLocalImageFileFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(localFileFetchProducer);
    }
    return mLocalImageFileFetchSequence;
  }

  /** Bitmap cache get -> thread hand off -> multiplex -> bitmap cache -> local video thumbnail */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getLocalVideoFileFetchSequence() {
    if (mLocalVideoFileFetchSequence == null) {
      LocalVideoThumbnailProducer localVideoThumbnailProducer =
          mProducerFactory.newLocalVideoThumbnailProducer();
      mLocalVideoFileFetchSequence =
          newBitmapCacheGetToBitmapCacheSequence(localVideoThumbnailProducer);
    }
    return mLocalVideoFileFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> thumbnail resize and rotate -> thumbnail branch -> local content
   * thumbnail creation -> exif thumbnail creation -> local image resize and rotate -> add meta data
   * producer -> multiplex -> encoded cache -> (webp transcode) -> local content uri fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getLocalContentUriFetchSequence() {
    if (mLocalContentUriFetchSequence == null) {
      LocalContentUriFetchProducer localContentUriFetchProducer =
          mProducerFactory.newLocalContentUriFetchProducer();

      ThumbnailProducer<EncodedImage>[] thumbnailProducers = new ThumbnailProducer[2];
      thumbnailProducers[0] = mProducerFactory.newLocalContentUriThumbnailFetchProducer();
      thumbnailProducers[1] = mProducerFactory.newLocalExifThumbnailProducer();

      mLocalContentUriFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(
              localContentUriFetchProducer, thumbnailProducers);
    }
    return mLocalContentUriFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) ->
   * qualified resource fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getQualifiedResourceFetchSequence() {
    if (mQualifiedResourceFetchSequence == null) {
      QualifiedResourceFetchProducer qualifiedResourceFetchProducer =
          mProducerFactory.newQualifiedResourceFetchProducer();
      mQualifiedResourceFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(qualifiedResourceFetchProducer);
    }
    return mQualifiedResourceFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * resource fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getLocalResourceFetchSequence() {
    if (mLocalResourceFetchSequence == null) {
      LocalResourceFetchProducer localResourceFetchProducer =
          mProducerFactory.newLocalResourceFetchProducer();
      mLocalResourceFetchSequence =
          newBitmapCacheGetToLocalTransformSequence(localResourceFetchProducer);
    }
    return mLocalResourceFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * asset fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>> getLocalAssetFetchSequence() {
    if (mLocalAssetFetchSequence == null) {
      LocalAssetFetchProducer localAssetFetchProducer =
          mProducerFactory.newLocalAssetFetchProducer();
      mLocalAssetFetchSequence = newBitmapCacheGetToLocalTransformSequence(localAssetFetchProducer);
    }
    return mLocalAssetFetchSequence;
  }

  /**
   * bitmap cache get -> background thread hand-off -> bitmap cache -> decode -> resize and rotate
   * -> (webp transcode) -> data fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>> getDataFetchSequence() {
    if (mDataFetchSequence == null) {
      Producer<EncodedImage> inputProducer = mProducerFactory.newDataFetchProducer();
      if (WebpSupportStatus.sIsWebpSupportRequired
          && (!mWebpSupportEnabled || WebpSupportStatus.sWebpBitmapFactory == null)) {
        inputProducer = mProducerFactory.newWebpTranscodeProducer(inputProducer);
      }
      inputProducer = mProducerFactory.newAddImageTransformMetaDataProducer(inputProducer);
      inputProducer =
          mProducerFactory.newResizeAndRotateProducer(inputProducer, true, mImageTranscoderFactory);
      mDataFetchSequence = newBitmapCacheGetToDecodeSequence(inputProducer);
    }
    return mDataFetchSequence;
  }

  /**
   * Creates a new fetch sequence that just needs the source producer.
   *
   * @param inputProducer the source producer
   * @return the new sequence
   */
  private Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToLocalTransformSequence(
      Producer<EncodedImage> inputProducer) {
    ThumbnailProducer<EncodedImage>[] defaultThumbnailProducers = new ThumbnailProducer[1];
    defaultThumbnailProducers[0] = mProducerFactory.newLocalExifThumbnailProducer();
    return newBitmapCacheGetToLocalTransformSequence(inputProducer, defaultThumbnailProducers);
  }

  /**
   * Creates a new fetch sequence that just needs the source producer.
   *
   * @param inputProducer the source producer
   * @param thumbnailProducers the thumbnail producers from which to request the image before
   *     falling back to the full image producer sequence
   * @return the new sequence
   */
  private Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToLocalTransformSequence(
      Producer<EncodedImage> inputProducer, ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    inputProducer = newEncodedCacheMultiplexToTranscodeSequence(inputProducer);
    Producer<EncodedImage> inputProducerAfterDecode =
        newLocalTransformationsSequence(inputProducer, thumbnailProducers);
    return newBitmapCacheGetToDecodeSequence(inputProducerAfterDecode);
  }

  /**
   * Same as {@code newBitmapCacheGetToBitmapCacheSequence} but with an extra DecodeProducer.
   *
   * @param inputProducer producer providing the input to the decode
   * @return bitmap cache get to decode sequence
   */
  private Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToDecodeSequence(
      Producer<EncodedImage> inputProducer) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#newBitmapCacheGetToDecodeSequence");
    }
    DecodeProducer decodeProducer = mProducerFactory.newDecodeProducer(inputProducer);
    Producer<CloseableReference<CloseableImage>> result =
        newBitmapCacheGetToBitmapCacheSequence(decodeProducer);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return result;
  }

  /**
   * encoded cache multiplex -> encoded cache -> (disk cache) -> (webp transcode)
   *
   * @param inputProducer producer providing the input to the transcode
   * @return encoded cache multiplex to webp transcode sequence
   */
  private Producer<EncodedImage> newEncodedCacheMultiplexToTranscodeSequence(
      Producer<EncodedImage> inputProducer) {
    if (WebpSupportStatus.sIsWebpSupportRequired
        && (!mWebpSupportEnabled || WebpSupportStatus.sWebpBitmapFactory == null)) {
      inputProducer = mProducerFactory.newWebpTranscodeProducer(inputProducer);
    }
    if (mDiskCacheEnabled) {
      inputProducer = newDiskCacheSequence(inputProducer);
    }
    EncodedMemoryCacheProducer encodedMemoryCacheProducer =
        mProducerFactory.newEncodedMemoryCacheProducer(inputProducer);
    if (mIsDiskCacheProbingEnabled) {
      EncodedProbeProducer probeProducer =
          mProducerFactory.newEncodedProbeProducer(encodedMemoryCacheProducer);
      return mProducerFactory.newEncodedCacheKeyMultiplexProducer(probeProducer);
    }
    return mProducerFactory.newEncodedCacheKeyMultiplexProducer(encodedMemoryCacheProducer);
  }

  private Producer<EncodedImage> newDiskCacheSequence(Producer<EncodedImage> inputProducer) {
    Producer<EncodedImage> cacheWriteProducer;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("ProducerSequenceFactory#newDiskCacheSequence");
    }
    if (mPartialImageCachingEnabled) {
      Producer<EncodedImage> partialDiskCacheProducer =
          mProducerFactory.newPartialDiskCacheProducer(inputProducer);
      cacheWriteProducer = mProducerFactory.newDiskCacheWriteProducer(partialDiskCacheProducer);
    } else {
      cacheWriteProducer = mProducerFactory.newDiskCacheWriteProducer(inputProducer);
    }
    DiskCacheReadProducer result = mProducerFactory.newDiskCacheReadProducer(cacheWriteProducer);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return result;
  }

  /**
   * Bitmap cache get -> thread hand off -> multiplex -> bitmap cache
   *
   * @param inputProducer producer providing the input to the bitmap cache
   * @return bitmap cache get to bitmap cache sequence
   */
  private Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToBitmapCacheSequence(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    BitmapMemoryCacheProducer bitmapMemoryCacheProducer =
        mProducerFactory.newBitmapMemoryCacheProducer(inputProducer);
    BitmapMemoryCacheKeyMultiplexProducer bitmapKeyMultiplexProducer =
        mProducerFactory.newBitmapMemoryCacheKeyMultiplexProducer(bitmapMemoryCacheProducer);
    Producer<CloseableReference<CloseableImage>> threadHandoffProducer =
        mProducerFactory.newBackgroundThreadHandoffProducer(
            bitmapKeyMultiplexProducer, mThreadHandoffProducerQueue);
    if (mIsEncodedMemoryCacheProbingEnabled || mIsDiskCacheProbingEnabled) {
      BitmapMemoryCacheGetProducer bitmapMemoryCacheGetProducer =
          mProducerFactory.newBitmapMemoryCacheGetProducer(threadHandoffProducer);
      return mProducerFactory.newBitmapProbeProducer(bitmapMemoryCacheGetProducer);
    }
    return mProducerFactory.newBitmapMemoryCacheGetProducer(threadHandoffProducer);
  }

  /**
   * Branch on separate images -> thumbnail resize and rotate -> thumbnail producers as provided ->
   * local image resize and rotate -> add meta data producer
   *
   * @param inputProducer producer providing the input to add meta data producer
   * @param thumbnailProducers the thumbnail producers from which to request the image before
   *     falling back to the full image producer sequence
   * @return local transformations sequence
   */
  private Producer<EncodedImage> newLocalTransformationsSequence(
      Producer<EncodedImage> inputProducer, ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    Producer<EncodedImage> localImageProducer =
        ProducerFactory.newAddImageTransformMetaDataProducer(inputProducer);
    localImageProducer =
        mProducerFactory.newResizeAndRotateProducer(
            localImageProducer, true, mImageTranscoderFactory);
    ThrottlingProducer<EncodedImage> localImageThrottlingProducer =
        mProducerFactory.newThrottlingProducer(localImageProducer);
    return mProducerFactory.newBranchOnSeparateImagesProducer(
        newLocalThumbnailProducer(thumbnailProducers), localImageThrottlingProducer);
  }

  private Producer<EncodedImage> newLocalThumbnailProducer(
      ThumbnailProducer<EncodedImage>[] thumbnailProducers) {
    ThumbnailBranchProducer thumbnailBranchProducer =
        mProducerFactory.newThumbnailBranchProducer(thumbnailProducers);
    return mProducerFactory.newResizeAndRotateProducer(
        thumbnailBranchProducer, true, mImageTranscoderFactory);
  }

  /** post-processor producer -> copy producer -> inputProducer */
  private synchronized Producer<CloseableReference<CloseableImage>> getPostprocessorSequence(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    if (!mPostprocessorSequences.containsKey(inputProducer)) {
      PostprocessorProducer postprocessorProducer =
          mProducerFactory.newPostprocessorProducer(inputProducer);
      PostprocessedBitmapMemoryCacheProducer postprocessedBitmapMemoryCacheProducer =
          mProducerFactory.newPostprocessorBitmapMemoryCacheProducer(postprocessorProducer);
      mPostprocessorSequences.put(inputProducer, postprocessedBitmapMemoryCacheProducer);
    }
    return mPostprocessorSequences.get(inputProducer);
  }

  /** swallow result producer -> inputProducer */
  private synchronized Producer<Void> getDecodedImagePrefetchSequence(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    if (!mCloseableImagePrefetchSequences.containsKey(inputProducer)) {
      SwallowResultProducer<CloseableReference<CloseableImage>> swallowResultProducer =
          mProducerFactory.newSwallowResultProducer(inputProducer);
      mCloseableImagePrefetchSequences.put(inputProducer, swallowResultProducer);
    }
    return mCloseableImagePrefetchSequences.get(inputProducer);
  }

  /** bitmap prepare producer -> inputProducer */
  private synchronized Producer<CloseableReference<CloseableImage>> getBitmapPrepareSequence(
      Producer<CloseableReference<CloseableImage>> inputProducer) {
    Producer<CloseableReference<CloseableImage>> bitmapPrepareProducer =
        mBitmapPrepareSequences.get(inputProducer);

    if (bitmapPrepareProducer == null) {
      bitmapPrepareProducer = mProducerFactory.newBitmapPrepareProducer(inputProducer);
      mBitmapPrepareSequences.put(inputProducer, bitmapPrepareProducer);
    }

    return bitmapPrepareProducer;
  }

  private static String getShortenedUriString(Uri uri) {
    final String uriString = String.valueOf(uri);
    return uriString.length() > 30 ? uriString.substring(0, 30) + "..." : uriString;
  }
}
