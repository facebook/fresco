/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import java.util.Map;

import android.net.Uri;
import android.os.Build;
import android.util.Pair;

import com.facebook.common.internal.Maps;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.producers.AddImageTransformMetaDataProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheKeyMultiplexProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.BranchOnSeparateImagesProducer;
import com.facebook.imagepipeline.producers.DecodeProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.ImageTransformMetaData;
import com.facebook.imagepipeline.producers.LocalAssetFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriFetchProducer;
import com.facebook.imagepipeline.producers.LocalExifThumbnailProducer;
import com.facebook.imagepipeline.producers.LocalFileFetchProducer;
import com.facebook.imagepipeline.producers.LocalResourceFetchProducer;
import com.facebook.imagepipeline.producers.LocalVideoThumbnailProducer;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.PostprocessorProducer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.ResizeAndRotateProducer;
import com.facebook.imagepipeline.producers.SwallowResultProducer;
import com.facebook.imagepipeline.producers.ThreadHandoffProducer;
import com.facebook.imagepipeline.producers.ThrottlingProducer;
import com.facebook.imagepipeline.request.ImageRequest;

public class ProducerSequenceFactory {
  private static final int MAX_SIMULTANEOUS_FILE_FETCH_AND_RESIZE = 5;

  private final ProducerFactory mProducerFactory;
  private final NetworkFetcher mNetworkFetcher;
  private final boolean mResizeAndRotateEnabledForNetwork;

  // Saved sequences
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mNetworkFetchSequence;
  @VisibleForTesting Producer<CloseableReference<PooledByteBuffer>>
      mBackgroundNetworkFetchToEncodedMemorySequence;
  @VisibleForTesting Producer<Void> mNetworkFetchToEncodedMemoryPrefetchSequence;
  private Producer<CloseableReference<PooledByteBuffer>>
      mCommonNetworkFetchToEncodedMemorySequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalImageFileFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalVideoFileFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalContentUriFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalResourceFetchSequence;
  @VisibleForTesting Producer<CloseableReference<CloseableImage>> mLocalAssetFetchSequence;
  @VisibleForTesting Map<
      Producer<CloseableReference<CloseableImage>>,
      Producer<CloseableReference<CloseableImage>>>
      mPostprocessorSequences;
  @VisibleForTesting Map<Producer<CloseableReference<CloseableImage>>, Producer<Void>>
      mCloseableImagePrefetchSequences;

  public ProducerSequenceFactory(
      ProducerFactory producerFactory,
      NetworkFetcher networkFetcher,
      boolean resizeAndRotateEnabledForNetwork) {
    mProducerFactory = producerFactory;
    mNetworkFetcher = networkFetcher;
    mResizeAndRotateEnabledForNetwork = resizeAndRotateEnabledForNetwork;
    mPostprocessorSequences = Maps.newHashMap();
    mCloseableImagePrefetchSequences = Maps.newHashMap();
  }

  /**
   * Returns a sequence that can be used for a request for an encoded image.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<CloseableReference<PooledByteBuffer>> getEncodedImageProducerSequence(
      ImageRequest imageRequest) {
    validateEncodedImageRequest(imageRequest);
    return getBackgroundNetworkFetchToEncodedMemorySequence();
  }

  /**
   * Returns a sequence that can be used for a prefetch request for an encoded image.
   *
   * <p>Guaranteed to return the same sequence as
   * {@code getEncodedImageProducerSequence(request)}, except that it is pre-pended with a
   * {@link SwallowResultProducer}.
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<Void> getEncodedImagePrefetchProducerSequence(ImageRequest imageRequest) {
    validateEncodedImageRequest(imageRequest);
    return getNetworkFetchToEncodedMemoryPrefetchSequence();
  }

  private static void validateEncodedImageRequest(ImageRequest imageRequest) {
    Preconditions.checkNotNull(imageRequest);
    Preconditions.checkArgument(UriUtil.isNetworkUri(imageRequest.getSourceUri()));
    Preconditions.checkArgument(
        imageRequest.getLowestPermittedRequestLevel().getValue() <=
            ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE.getValue());
  }

  /**
   * Returns a sequence that can be used for a request for a decoded image.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<CloseableReference<CloseableImage>> getDecodedImageProducerSequence(
      ImageRequest imageRequest) {
    Producer<CloseableReference<CloseableImage>> pipelineSequence =
        getBasicDecodedImageSequence(imageRequest);
    if (imageRequest.getPostprocessor() != null) {
      return getPostprocessorSequence(pipelineSequence);
    } else {
      return pipelineSequence;
    }
  }

  /**
   * Returns a sequence that can be used for a prefetch request for a decoded image.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  public Producer<Void> getDecodedImagePrefetchProducerSequence(
      ImageRequest imageRequest) {
    return getDecodedImagePrefetchSequence(getBasicDecodedImageSequence(imageRequest));
  }

  private Producer<CloseableReference<CloseableImage>> getBasicDecodedImageSequence(
      ImageRequest imageRequest) {
    Preconditions.checkNotNull(imageRequest);

    Uri uri = imageRequest.getSourceUri();
    Preconditions.checkNotNull(uri, "Uri is null.");
    if (UriUtil.isNetworkUri(uri)) {
      return getNetworkFetchSequence();
    } else if (UriUtil.isLocalFileUri(uri)) {
      if (MediaUtils.isVideo(MediaUtils.extractMime(uri.getPath()))) {
        return getLocalVideoFileFetchSequence();
      } else {
        return getLocalImageFileFetchSequence();
      }
    } else if (UriUtil.isLocalContentUri(uri)) {
      return getLocalContentUriFetchSequence();
    } else if (UriUtil.isLocalAssetUri(uri)) {
      return getLocalAssetFetchSequence();
    } else if (UriUtil.isLocalResourceUri(uri)) {
      return getLocalResourceFetchSequence();
    } else {
      String uriString = uri.toString();
      if (uriString.length() > 30) {
        uriString = uriString.substring(0, 30) + "...";
      }
      throw new RuntimeException("Unsupported uri scheme! Uri is: " + uriString);
    }
  }

  /**
   * swallow result if prefetch -> bitmap cache get -> wait if scrolling ->
   * background thread hand-off -> multiplex -> bitmap cache -> decode -> multiplex ->
   * encoded cache -> disk cache -> (webp transcode) -> network fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>> getNetworkFetchSequence() {
    if (mNetworkFetchSequence == null) {
      mNetworkFetchSequence =
          newBitmapCacheGetToDecodeSequence(getCommonNetworkFetchToEncodedMemorySequence());
    }
    return mNetworkFetchSequence;
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache ->
   * disk cache -> (webp transcode) -> network fetch.
   */
  private synchronized Producer<CloseableReference<PooledByteBuffer>>
      getBackgroundNetworkFetchToEncodedMemorySequence() {
    if (mBackgroundNetworkFetchToEncodedMemorySequence == null) {
      // Use hand-off producer to ensure that we don't do any unnecessary work on the UI thread.
      mBackgroundNetworkFetchToEncodedMemorySequence =
          mProducerFactory.newBackgroundThreadHandoffProducer(
              getCommonNetworkFetchToEncodedMemorySequence());
    }
    return mBackgroundNetworkFetchToEncodedMemorySequence;
  }

  /**
   * swallow-result -> background-thread hand-off -> multiplex -> encoded cache ->
   * disk cache -> (webp transcode) -> network fetch.
   */
  private synchronized Producer<Void> getNetworkFetchToEncodedMemoryPrefetchSequence() {
    if (mNetworkFetchToEncodedMemoryPrefetchSequence == null) {
      mNetworkFetchToEncodedMemoryPrefetchSequence =
          mProducerFactory.newSwallowResultProducer(
              getBackgroundNetworkFetchToEncodedMemorySequence());
    }
    return mNetworkFetchToEncodedMemoryPrefetchSequence;
  }

  /**
   * multiplex -> encoded cache -> disk cache -> (webp transcode) -> network fetch.
   */
  private synchronized Producer<CloseableReference<PooledByteBuffer>>
      getCommonNetworkFetchToEncodedMemorySequence() {
    if (mCommonNetworkFetchToEncodedMemorySequence == null) {
      mCommonNetworkFetchToEncodedMemorySequence = newEncodedCacheMultiplexToTranscodeSequence(
          mProducerFactory.newNetworkFetchProducer(mNetworkFetcher));
      if (mResizeAndRotateEnabledForNetwork) {
        mCommonNetworkFetchToEncodedMemorySequence =
            newResizeAndRotateImagesSequence(mCommonNetworkFetchToEncodedMemorySequence);
      }
    }
    return mCommonNetworkFetchToEncodedMemorySequence;
  }

  /**
   * bitmap cache get -> wait if scrolling ->
   * background thread hand-off -> multiplex -> bitmap cache -> decode ->
   * branch on separate images
   *   -> exif resize and rotate -> exif thumbnail creation
   *   -> local image resize and rotate -> add meta data producer -> multiplex -> encoded cache ->
   *   (webp transcode) -> local file fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getLocalImageFileFetchSequence() {
    if (mLocalImageFileFetchSequence == null) {
      LocalFileFetchProducer localFileFetchProducer =
          mProducerFactory.newLocalFileFetchProducer();
      mLocalImageFileFetchSequence =
          newBitmapCacheGetToTranscodeSequence(localFileFetchProducer, /* isLocal */true);
    }
    return mLocalImageFileFetchSequence;
  }

  /**
   * Bitmap cache get -> wait if scrolling -> thread hand off -> multiplex -> bitmap cache ->
   * local video thumbnail
   */
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
   * bitmap cache get -> wait if scrolling ->
   * background thread hand-off -> multiplex -> bitmap cache -> decode ->
   * branch on separate images
   *   -> exif resize and rotate -> exif thumbnail creation
   *   -> local image resize and rotate -> add meta data producer -> multiplex -> encoded cache ->
   *   (webp transcode) -> local content uri fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getLocalContentUriFetchSequence() {
    if (mLocalContentUriFetchSequence == null) {
      LocalContentUriFetchProducer localContentUriFetchProducer =
          mProducerFactory.newContentUriFetchProducer();
      mLocalContentUriFetchSequence =
          newBitmapCacheGetToTranscodeSequence(localContentUriFetchProducer, /* isLocal */true);
    }
    return mLocalContentUriFetchSequence;
  }

  /**
   * bitmap cache get -> wait if scrolling ->
   * background thread hand-off -> multiplex -> bitmap cache -> decode ->
   * branch on separate images
   *   -> exif resize and rotate -> exif thumbnail creation
   *   -> local image resize and rotate -> add meta data producer -> multiplex -> encoded cache ->
   *   (webp transcode) -> local resource fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>>
      getLocalResourceFetchSequence() {
    if (mLocalResourceFetchSequence == null) {
      LocalResourceFetchProducer localResourceFetchProducer =
          mProducerFactory.newLocalResourceFetchProducer();
      mLocalResourceFetchSequence =
          newBitmapCacheGetToTranscodeSequence(localResourceFetchProducer, /* isLocal */true);
    }
    return mLocalResourceFetchSequence;
  }

  /**
   * bitmap cache get -> wait if scrolling ->
   * background thread hand-off -> multiplex -> bitmap cache -> decode ->
   * branch on separate images
   *   -> exif resize and rotate -> exif thumbnail creation
   *   -> local image resize and rotate -> add meta data producer -> multiplex -> encoded cache ->
   *   (webp transcode) -> local asset fetch.
   */
  private synchronized Producer<CloseableReference<CloseableImage>> getLocalAssetFetchSequence() {
    if (mLocalAssetFetchSequence == null) {
      LocalAssetFetchProducer localAssetFetchProducer =
          mProducerFactory.newLocalAssetFetchProducer();
      mLocalAssetFetchSequence =
          newBitmapCacheGetToTranscodeSequence(localAssetFetchProducer, /* isLocal */true);
    }
    return mLocalAssetFetchSequence;
  }

  /**
   * Creates a new fetch sequence that just needs the source producer.
   * @param nextProducer the source producer
   * @param isLocal whether the image source is local or not
   * @return the new sequence
   */
  private Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToTranscodeSequence(
      Producer<CloseableReference<PooledByteBuffer>> nextProducer,
      boolean isLocal) {
    Producer<CloseableReference<PooledByteBuffer>> nextProducerAfterDecode =
        newEncodedCacheMultiplexToTranscodeSequence(nextProducer);
    if (isLocal) {
      nextProducerAfterDecode = newLocalTransformationsSequence(nextProducerAfterDecode);
    }
    return newBitmapCacheGetToDecodeSequence(nextProducerAfterDecode);
  }

  /**
   * Same as {@code newBitmapCacheGetToBitmapCacheSequence} but with an extra DecodeProducer.
   * @param nextProducer next producer in the sequence after decode
   * @return bitmap cache get to decode sequence
   */
  private Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToDecodeSequence(
      Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    DecodeProducer decodeProducer = mProducerFactory.newDecodeProducer(nextProducer);
    return newBitmapCacheGetToBitmapCacheSequence(decodeProducer);
  }

  /**
   * encoded cache multiplex -> encoded cache -> (disk cache) -> (webp transcode)
   * @param nextProducer next producer in the sequence
   * @return encoded cache multiplex to webp transcode sequence
   */
  private Producer<CloseableReference<PooledByteBuffer>>
      newEncodedCacheMultiplexToTranscodeSequence(
          Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      nextProducer = mProducerFactory.newWebpTranscodeProducer(nextProducer);
    }
    nextProducer = mProducerFactory.newDiskCacheProducer(nextProducer);
    EncodedMemoryCacheProducer encodedMemoryCacheProducer =
        mProducerFactory.newEncodedMemoryCacheProducer(nextProducer);
    return mProducerFactory.newEncodedCacheKeyMultiplexProducer(encodedMemoryCacheProducer);
  }

  /**
   * Bitmap cache get -> wait if scrolling -> thread hand off -> multiplex -> bitmap cache
   * @param nextProducer next producer in the sequence after bitmap cache
   * @return bitmap cache get to bitmap cache sequence
   */
  private Producer<CloseableReference<CloseableImage>> newBitmapCacheGetToBitmapCacheSequence(
      Producer<CloseableReference<CloseableImage>> nextProducer) {
    BitmapMemoryCacheProducer bitmapMemoryCacheProducer =
        mProducerFactory.newBitmapMemoryCacheProducer(nextProducer);
    BitmapMemoryCacheKeyMultiplexProducer bitmapKeyMultiplexProducer =
        mProducerFactory.newBitmapMemoryCacheKeyMultiplexProducer(bitmapMemoryCacheProducer);
    ThreadHandoffProducer<CloseableReference<CloseableImage>> threadHandoffProducer =
        mProducerFactory.newBackgroundThreadHandoffProducer(bitmapKeyMultiplexProducer);
    return mProducerFactory.newBitmapMemoryCacheGetProducer(threadHandoffProducer);
  }

  private Producer<CloseableReference<PooledByteBuffer>>
      newResizeAndRotateImagesSequence(
          Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    AddImageTransformMetaDataProducer addImageTransformMetaDataProducer =
        ProducerFactory.newAddImageTransformMetaDataProducer(nextProducer);
    ResizeAndRotateProducer networkImageResizeAndRotateProducer =
        mProducerFactory.newResizeAndRotateProducer(addImageTransformMetaDataProducer);
    return ProducerFactory.newRemoveImageTransformMetaDataProducer(
            networkImageResizeAndRotateProducer);
  }

  /**
   * Remove image transform meta data -> branch on separate images
   *   -> exif resize and rotate -> exif thumbnail creation
   *   -> local image resize and rotate -> add meta data producer
   * @param nextProducer next producer in the sequence after add meta data producer
   * @return local transformations sequence
   */
  private Producer<CloseableReference<PooledByteBuffer>> newLocalTransformationsSequence(
      Producer<CloseableReference<PooledByteBuffer>> nextProducer) {
    AddImageTransformMetaDataProducer addImageTransformMetaDataProducer =
        mProducerFactory.newAddImageTransformMetaDataProducer(nextProducer);
    ResizeAndRotateProducer localImageResizeAndRotateProducer =
        mProducerFactory.newResizeAndRotateProducer(addImageTransformMetaDataProducer);
    ThrottlingProducer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>>
        localImageThrottlingProducer =
        mProducerFactory.newThrottlingProducer(
            MAX_SIMULTANEOUS_FILE_FETCH_AND_RESIZE,
            localImageResizeAndRotateProducer);
    LocalExifThumbnailProducer localExifThumbnailProducer =
        mProducerFactory.newLocalExifThumbnailProducer();
    ResizeAndRotateProducer exifThumbnailResizeAndRotateProducer =
        mProducerFactory.newResizeAndRotateProducer(localExifThumbnailProducer);
    BranchOnSeparateImagesProducer branchOnSeparateImagesProducer =
        mProducerFactory.newBranchOnSeparateImagesProducer(
            exifThumbnailResizeAndRotateProducer,
            localImageThrottlingProducer);
    return mProducerFactory.newRemoveImageTransformMetaDataProducer(branchOnSeparateImagesProducer);

  }

  /**
   * post-processor producer -> copy producer -> next producer
   */
  private synchronized Producer<CloseableReference<CloseableImage>> getPostprocessorSequence(
      Producer<CloseableReference<CloseableImage>> nextProducer) {
    if (!mPostprocessorSequences.containsKey(nextProducer)) {
      PostprocessorProducer postprocessorProducer =
          mProducerFactory.newPostprocessorProducer(nextProducer);
      mPostprocessorSequences.put(nextProducer, postprocessorProducer);
    }
    return mPostprocessorSequences.get(nextProducer);
  }

  /**
   * swallow result producer -> next producer
   */
  private synchronized Producer<Void> getDecodedImagePrefetchSequence(
      Producer<CloseableReference<CloseableImage>> nextProducer) {
    if (!mCloseableImagePrefetchSequences.containsKey(nextProducer)) {
      SwallowResultProducer<CloseableReference<CloseableImage>> swallowResultProducer =
          mProducerFactory.newSwallowResultProducer(nextProducer);
      mCloseableImagePrefetchSequences.put(nextProducer, swallowResultProducer);
    }
    return mCloseableImagePrefetchSequences.get(nextProducer);
  }
}
