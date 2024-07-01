/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.facebook.common.internal.Preconditions
import com.facebook.common.media.MediaUtils.isVideo
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.common.SourceUriType
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.CustomProducerSequenceFactory
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.producers.Producer
import com.facebook.imagepipeline.producers.RemoveImageTransformMetaDataProducer
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue
import com.facebook.imagepipeline.producers.ThumbnailProducer
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.systrace.FrescoSystrace.traceSection
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory

class ProducerSequenceFactory(
    private val contentResolver: ContentResolver,
    private val producerFactory: ProducerFactory,
    private val networkFetcher: NetworkFetcher<*>,
    private val resizeAndRotateEnabledForNetwork: Boolean,
    private val webpSupportEnabled: Boolean,
    private val threadHandoffProducerQueue: ThreadHandoffProducerQueue,
    private val downsampleMode: DownsampleMode,
    private val useBitmapPrepareToDraw: Boolean,
    private val partialImageCachingEnabled: Boolean,
    private val diskCacheEnabled: Boolean,
    private val imageTranscoderFactory: ImageTranscoderFactory,
    private val isEncodedMemoryCacheProbingEnabled: Boolean,
    private val isDiskCacheProbingEnabled: Boolean,
    private val allowDelay: Boolean,
    private val customProducerSequenceFactories: Set<CustomProducerSequenceFactory>?
) {

  @VisibleForTesting
  var postprocessorSequences:
      MutableMap<
          Producer<CloseableReference<CloseableImage>>,
          Producer<CloseableReference<CloseableImage>>> =
      mutableMapOf()

  @VisibleForTesting
  var closeableImagePrefetchSequences:
      MutableMap<Producer<CloseableReference<CloseableImage>>, Producer<Void?>> =
      mutableMapOf()

  @VisibleForTesting
  var bitmapPrepareSequences:
      MutableMap<
          Producer<CloseableReference<CloseableImage>>,
          Producer<CloseableReference<CloseableImage>>> =
      mutableMapOf()

  /**
   * Returns a sequence that can be used for a request for an encoded image from either network or
   * local files.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  fun getEncodedImageProducerSequence(
      imageRequest: ImageRequest
  ): Producer<CloseableReference<PooledByteBuffer>> =
      traceSection("ProducerSequenceFactory#getEncodedImageProducerSequence") {
        validateEncodedImageRequest(imageRequest)
        val uri = imageRequest.sourceUri
        when (imageRequest.sourceUriType) {
          SourceUriType.SOURCE_TYPE_NETWORK -> networkFetchEncodedImageProducerSequence
          SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE,
          SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE -> localFileFetchEncodedImageProducerSequence
          SourceUriType.SOURCE_TYPE_LOCAL_CONTENT ->
              localContentUriFetchEncodedImageProducerSequence
          else -> {
            if (customProducerSequenceFactories != null) {
              for (customProducerSequenceFactory in customProducerSequenceFactories) {
                val sequence =
                    customProducerSequenceFactory.getCustomEncodedImageSequence(
                        imageRequest, this, producerFactory, threadHandoffProducerQueue)
                if (sequence != null) {
                  return sequence
                }
              }
            }
            throw IllegalArgumentException(
                "Unsupported uri scheme for encoded image fetch! Uri is: " +
                    getShortenedUriString(uri))
          }
        }
      }

  /** Returns a sequence that can be used for a request for an encoded image from network. */
  val networkFetchEncodedImageProducerSequence:
      Producer<CloseableReference<PooledByteBuffer>> by lazy {
    traceSection("ProducerSequenceFactory#getNetworkFetchEncodedImageProducerSequence:init") {
      RemoveImageTransformMetaDataProducer(backgroundNetworkFetchToEncodedMemorySequence)
    }
  }

  /** Returns a sequence that can be used for a request for an encoded image from a local file. */
  @VisibleForTesting
  val localFileFetchEncodedImageProducerSequence:
      Producer<CloseableReference<PooledByteBuffer>> by lazy {
    traceSection("ProducerSequenceFactory#getLocalFileFetchEncodedImageProducerSequence:init") {
      RemoveImageTransformMetaDataProducer(backgroundLocalFileFetchToEncodeMemorySequence)
    }
  }

  /**
   * Returns a sequence that can be used for a request for an encoded image from a local content
   * uri.
   */
  val localContentUriFetchEncodedImageProducerSequence:
      Producer<CloseableReference<PooledByteBuffer>> by lazy {
    traceSection(
        "ProducerSequenceFactory#getLocalContentUriFetchEncodedImageProducerSequence:init") {
          RemoveImageTransformMetaDataProducer(backgroundLocalContentUriFetchToEncodeMemorySequence)
        }
  }

  /**
   * Returns a sequence that can be used for a prefetch request for an encoded image.
   *
   * Guaranteed to return the same sequence as `getEncodedImageProducerSequence(request)`, except
   * that it is pre-pended with a [SwallowResultProducer].
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  fun getEncodedImagePrefetchProducerSequence(imageRequest: ImageRequest): Producer<Void?> {
    validateEncodedImageRequest(imageRequest)
    return when (imageRequest.sourceUriType) {
      SourceUriType.SOURCE_TYPE_NETWORK -> networkFetchToEncodedMemoryPrefetchSequence
      SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE,
      SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE -> localFileFetchToEncodedMemoryPrefetchSequence
      else -> {
        val uri = imageRequest.sourceUri
        throw IllegalArgumentException(
            "Unsupported uri scheme for encoded image fetch! Uri is: " + getShortenedUriString(uri))
      }
    }
  }

  /**
   * Returns a sequence that can be used for a request for a decoded image.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  fun getDecodedImageProducerSequence(
      imageRequest: ImageRequest
  ): Producer<CloseableReference<CloseableImage>> =
      traceSection("ProducerSequenceFactory#getDecodedImageProducerSequence") {
        var pipelineSequence = getBasicDecodedImageSequence(imageRequest)
        if (imageRequest.postprocessor != null) {
          pipelineSequence = getPostprocessorSequence(pipelineSequence)
        }
        if (useBitmapPrepareToDraw) {
          pipelineSequence = getBitmapPrepareSequence(pipelineSequence)
        }
        if (allowDelay && imageRequest.delayMs > 0) {
          pipelineSequence = getDelaySequence(pipelineSequence)
        }
        return pipelineSequence
      }

  /**
   * Returns a sequence that can be used for a prefetch request for a decoded image.
   *
   * @param imageRequest the request that will be submitted
   * @return the sequence that should be used to process the request
   */
  fun getDecodedImagePrefetchProducerSequence(imageRequest: ImageRequest): Producer<Void?> {
    var inputProducer = getBasicDecodedImageSequence(imageRequest)
    if (useBitmapPrepareToDraw) {
      inputProducer = getBitmapPrepareSequence(inputProducer)
    }
    return getDecodedImagePrefetchSequence(inputProducer)
  }

  private fun getBasicDecodedImageSequence(
      imageRequest: ImageRequest
  ): Producer<CloseableReference<CloseableImage>> =
      traceSection("ProducerSequenceFactory#getBasicDecodedImageSequence") {
        val uri = imageRequest.sourceUri
        checkNotNull(uri) { "Uri is null." }
        when (imageRequest.sourceUriType) {
          SourceUriType.SOURCE_TYPE_NETWORK -> networkFetchSequence
          SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE -> {
            if (imageRequest.loadThumbnailOnlyForAndroidSdkAboveQ) {
              return localThumbnailBitmapSdk29FetchSequence
            } else {
              localVideoFileFetchSequence
            }
          }
          SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE -> {
            if (imageRequest.loadThumbnailOnlyForAndroidSdkAboveQ) {
              return localThumbnailBitmapSdk29FetchSequence
            } else {
              localImageFileFetchSequence
            }
          }
          SourceUriType.SOURCE_TYPE_LOCAL_CONTENT -> {
            if (imageRequest.loadThumbnailOnlyForAndroidSdkAboveQ) {
              return localThumbnailBitmapSdk29FetchSequence
            } else if (isVideo(contentResolver.getType(uri))) {
              return localVideoFileFetchSequence
            }
            localContentUriFetchSequence
          }
          SourceUriType.SOURCE_TYPE_LOCAL_ASSET -> localAssetFetchSequence
          SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE -> localResourceFetchSequence
          SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE -> qualifiedResourceFetchSequence
          SourceUriType.SOURCE_TYPE_DATA -> dataFetchSequence
          else -> {
            if (customProducerSequenceFactories != null) {
              for (customProducerSequenceFactory in customProducerSequenceFactories) {
                val sequence =
                    customProducerSequenceFactory.getCustomDecodedImageSequence(
                        imageRequest,
                        this,
                        producerFactory,
                        threadHandoffProducerQueue,
                        isEncodedMemoryCacheProbingEnabled,
                        isDiskCacheProbingEnabled)
                if (sequence != null) {
                  return sequence
                }
              }
            }
            throw IllegalArgumentException(
                "Unsupported uri scheme! Uri is: " + getShortenedUriString(uri))
          }
        }
      }

  /**
   * swallow result if prefetch -> bitmap cache get -> background thread hand-off -> multiplex ->
   * bitmap cache -> decode -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * network fetch.
   */
  val networkFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    traceSection("ProducerSequenceFactory#getNetworkFetchSequence:init") {
      newBitmapCacheGetToDecodeSequence(commonNetworkFetchToEncodedMemorySequence)
    }
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * network fetch.
   */
  val backgroundNetworkFetchToEncodedMemorySequence: Producer<EncodedImage?> by lazy {
    traceSection("ProducerSequenceFactory#getBackgroundNetworkFetchToEncodedMemorySequence:init") {
      // Use hand-off producer to ensure that we don't do any unnecessary work on the UI thread.
      producerFactory.newBackgroundThreadHandoffProducer(
          commonNetworkFetchToEncodedMemorySequence, threadHandoffProducerQueue)
    }
  }

  /**
   * swallow-result -> background-thread hand-off -> multiplex -> encoded cache -> disk cache ->
   * (webp transcode) -> network fetch.
   */
  val networkFetchToEncodedMemoryPrefetchSequence: Producer<Void?> by lazy {
    traceSection("ProducerSequenceFactory#getNetworkFetchToEncodedMemoryPrefetchSequence") {
      producerFactory.newSwallowResultProducer(backgroundNetworkFetchToEncodedMemorySequence)
    }
  }

  /**
   * multiplex -> encoded cache -> disk cache -> (webp transcode) -> network fetch. Alternatively,
   * multiplex -> combined network and cache
   */
  val commonNetworkFetchToEncodedMemorySequence: Producer<EncodedImage> by lazy {
    traceSection("ProducerSequenceFactory#getCommonNetworkFetchToEncodedMemorySequence") {
      newCommonNetworkFetchToEncodedMemorySequence(networkFetcher)
    }
  }

  @Synchronized
  fun newCommonNetworkFetchToEncodedMemorySequence(
      networkFetcher: NetworkFetcher<*>
  ): Producer<EncodedImage> =
      traceSection("ProducerSequenceFactory#createCommonNetworkFetchToEncodedMemorySequence") {
        val inputProducer: Producer<EncodedImage> =
            newEncodedCacheMultiplexToTranscodeSequence(
                producerFactory.newNetworkFetchProducer(networkFetcher))
        var networkFetchToEncodedMemorySequence: Producer<EncodedImage?> =
            ProducerFactory.newAddImageTransformMetaDataProducer(inputProducer)
        networkFetchToEncodedMemorySequence =
            producerFactory.newResizeAndRotateProducer(
                networkFetchToEncodedMemorySequence,
                resizeAndRotateEnabledForNetwork && downsampleMode != DownsampleMode.NEVER,
                imageTranscoderFactory)
        return networkFetchToEncodedMemorySequence
      }

  /**
   * swallow-result -> background-thread hand-off -> multiplex -> encoded cache -> disk cache ->
   * (webp transcode) -> local file fetch.
   */
  val localFileFetchToEncodedMemoryPrefetchSequence: Producer<Void?> by lazy {
    traceSection("ProducerSequenceFactory#getLocalFileFetchToEncodedMemoryPrefetchSequence:init") {
      producerFactory.newSwallowResultProducer(backgroundLocalFileFetchToEncodeMemorySequence)
    }
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * local file fetch
   */
  val backgroundLocalFileFetchToEncodeMemorySequence: Producer<EncodedImage?> by lazy {
    traceSection("ProducerSequenceFactory#getBackgroundLocalFileFetchToEncodeMemorySequence") {
      val localFileFetchProducer = producerFactory.newLocalFileFetchProducer()
      val toEncodedMultiplexProducer =
          newEncodedCacheMultiplexToTranscodeSequence(localFileFetchProducer)
      producerFactory.newBackgroundThreadHandoffProducer(
          toEncodedMultiplexProducer, threadHandoffProducerQueue)
    }
  }

  /**
   * background-thread hand-off -> multiplex -> encoded cache -> disk cache -> (webp transcode) ->
   * local content resolver fetch
   */
  val backgroundLocalContentUriFetchToEncodeMemorySequence: Producer<EncodedImage?> by lazy {
    traceSection(
        "ProducerSequenceFactory#getBackgroundLocalContentUriFetchToEncodeMemorySequence:init") {
          val localFileFetchProducer = producerFactory.newLocalContentUriFetchProducer()
          val toEncodedMultiplexProducer =
              newEncodedCacheMultiplexToTranscodeSequence(localFileFetchProducer)
          producerFactory.newBackgroundThreadHandoffProducer(
              toEncodedMultiplexProducer, threadHandoffProducerQueue)
        }
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * file fetch.
   */
  val localImageFileFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    val localFileFetchProducer = producerFactory.newLocalFileFetchProducer()
    newBitmapCacheGetToLocalTransformSequence(localFileFetchProducer)
  }

  /** Bitmap cache get -> thread hand off -> multiplex -> bitmap cache -> local video thumbnail */
  val localVideoFileFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    val localVideoThumbnailProducer = producerFactory.newLocalVideoThumbnailProducer()
    newBitmapCacheGetToBitmapCacheSequence(localVideoThumbnailProducer)
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> thumbnail resize and rotate -> thumbnail branch -> local content
   * thumbnail creation -> exif thumbnail creation -> local image resize and rotate -> add meta data
   * producer -> multiplex -> encoded cache -> (webp transcode) -> local content uri fetch.
   */
  val localContentUriFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    val localContentUriFetchProducer = producerFactory.newLocalContentUriFetchProducer()
    val thumbnailProducers: Array<ThumbnailProducer<EncodedImage>> =
        arrayOf(
            producerFactory.newLocalContentUriThumbnailFetchProducer(),
            producerFactory.newLocalExifThumbnailProducer(),
        )
    newBitmapCacheGetToLocalTransformSequence(localContentUriFetchProducer, thumbnailProducers)
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> local thumbnail
   * bitmap
   */
  @get:RequiresApi(Build.VERSION_CODES.Q)
  val localThumbnailBitmapSdk29FetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      newBitmapCacheGetToBitmapCacheSequence(producerFactory.newLocalThumbnailBitmapSdk29Producer())
    } else {
      throw Throwable("Unreachable exception. Just to make linter happy for the lazy block.")
    }
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) ->
   * qualified resource fetch.
   */
  val qualifiedResourceFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    val qualifiedResourceFetchProducer = producerFactory.newQualifiedResourceFetchProducer()
    newBitmapCacheGetToLocalTransformSequence(qualifiedResourceFetchProducer)
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * resource fetch.
   */
  val localResourceFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    val localResourceFetchProducer = producerFactory.newLocalResourceFetchProducer()
    newBitmapCacheGetToLocalTransformSequence(localResourceFetchProducer)
  }

  /**
   * bitmap cache get -> background thread hand-off -> multiplex -> bitmap cache -> decode -> branch
   * on separate images -> exif resize and rotate -> exif thumbnail creation -> local image resize
   * and rotate -> add meta data producer -> multiplex -> encoded cache -> (webp transcode) -> local
   * asset fetch.
   */
  val localAssetFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    val localAssetFetchProducer = producerFactory.newLocalAssetFetchProducer()
    newBitmapCacheGetToLocalTransformSequence(localAssetFetchProducer)
  }

  /**
   * bitmap cache get -> background thread hand-off -> bitmap cache -> decode -> resize and rotate
   * -> (webp transcode) -> data fetch.
   */
  val dataFetchSequence: Producer<CloseableReference<CloseableImage>> by lazy {
    var inputProducer: Producer<EncodedImage?> = producerFactory.newDataFetchProducer()
    inputProducer = ProducerFactory.newAddImageTransformMetaDataProducer(inputProducer)
    inputProducer =
        producerFactory.newResizeAndRotateProducer(inputProducer, true, imageTranscoderFactory)
    newBitmapCacheGetToDecodeSequence(inputProducer)
  }

  /**
   * Creates a new fetch sequence that just needs the source producer.
   *
   * @param inputProducer the source producer
   * @return the new sequence
   */
  private fun newBitmapCacheGetToLocalTransformSequence(
      inputProducer: Producer<EncodedImage>
  ): Producer<CloseableReference<CloseableImage>> {
    val defaultThumbnailProducers: Array<ThumbnailProducer<EncodedImage>> =
        arrayOf(producerFactory.newLocalExifThumbnailProducer())
    return newBitmapCacheGetToLocalTransformSequence(inputProducer, defaultThumbnailProducers)
  }

  /**
   * Creates a new fetch sequence that just needs the source producer.
   *
   * @param inputProducer the source producer
   * @param thumbnailProducers the thumbnail producers from which to request the image before
   *   falling back to the full image producer sequence
   * @return the new sequence
   */
  private fun newBitmapCacheGetToLocalTransformSequence(
      inputProducer: Producer<EncodedImage>,
      thumbnailProducers: Array<ThumbnailProducer<EncodedImage>>
  ): Producer<CloseableReference<CloseableImage>> {
    var ip = inputProducer
    ip = newEncodedCacheMultiplexToTranscodeSequence(ip)
    ip = newLocalTransformationsSequence(ip, thumbnailProducers)
    return newBitmapCacheGetToDecodeSequence(ip)
  }

  /**
   * Same as `newBitmapCacheGetToBitmapCacheSequence` but with an extra DecodeProducer.
   *
   * @param inputProducer producer providing the input to the decode
   * @return bitmap cache get to decode sequence
   */
  fun newBitmapCacheGetToDecodeSequence(
      inputProducer: Producer<EncodedImage>
  ): Producer<CloseableReference<CloseableImage>> {
    traceSection("ProducerSequenceFactory#newBitmapCacheGetToDecodeSequence") {
      val decodeProducer = producerFactory.newDecodeProducer(inputProducer)
      return newBitmapCacheGetToBitmapCacheSequence(decodeProducer)
    }
  }

  /**
   * encoded cache multiplex -> encoded cache -> (disk cache) -> (webp transcode)
   *
   * @param inputProducer producer providing the input to the transcode
   * @return encoded cache multiplex to webp transcode sequence
   */
  private fun newEncodedCacheMultiplexToTranscodeSequence(
      inputProducer: Producer<EncodedImage>
  ): Producer<EncodedImage> {
    var ip = inputProducer
    if (diskCacheEnabled) {
      ip = newDiskCacheSequence(ip)
    }
    val encodedMemoryCacheProducer = producerFactory.newEncodedMemoryCacheProducer(ip)
    if (isDiskCacheProbingEnabled) {
      val probeProducer = producerFactory.newEncodedProbeProducer(encodedMemoryCacheProducer)
      return producerFactory.newEncodedCacheKeyMultiplexProducer(probeProducer)
    }
    return producerFactory.newEncodedCacheKeyMultiplexProducer(encodedMemoryCacheProducer)
  }

  private fun newDiskCacheSequence(inputProducer: Producer<EncodedImage>): Producer<EncodedImage> =
      traceSection("ProducerSequenceFactory#newDiskCacheSequence") {
        val cacheWriteProducer =
            if (partialImageCachingEnabled) {
              val partialDiskCacheProducer: Producer<EncodedImage> =
                  producerFactory.newPartialDiskCacheProducer(inputProducer)
              producerFactory.newDiskCacheWriteProducer(partialDiskCacheProducer)
            } else {
              producerFactory.newDiskCacheWriteProducer(inputProducer)
            }
        return producerFactory.newDiskCacheReadProducer(cacheWriteProducer)
      }

  /**
   * Bitmap cache get -> thread hand off -> multiplex -> bitmap cache
   *
   * @param inputProducer producer providing the input to the bitmap cache
   * @return bitmap cache get to bitmap cache sequence
   */
  private fun newBitmapCacheGetToBitmapCacheSequence(
      inputProducer: Producer<CloseableReference<CloseableImage>>
  ): Producer<CloseableReference<CloseableImage>> {
    val bitmapMemoryCacheProducer = producerFactory.newBitmapMemoryCacheProducer(inputProducer)
    val bitmapKeyMultiplexProducer =
        producerFactory.newBitmapMemoryCacheKeyMultiplexProducer(bitmapMemoryCacheProducer)
    val threadHandoffProducer =
        producerFactory.newBackgroundThreadHandoffProducer(
            bitmapKeyMultiplexProducer, threadHandoffProducerQueue)
    if (isEncodedMemoryCacheProbingEnabled || isDiskCacheProbingEnabled) {
      val bitmapMemoryCacheGetProducer =
          producerFactory.newBitmapMemoryCacheGetProducer(threadHandoffProducer)
      return producerFactory.newBitmapProbeProducer(bitmapMemoryCacheGetProducer)
    }
    return producerFactory.newBitmapMemoryCacheGetProducer(threadHandoffProducer)
  }

  /**
   * Branch on separate images -> thumbnail resize and rotate -> thumbnail producers as provided ->
   * local image resize and rotate -> add meta data producer
   *
   * @param inputProducer producer providing the input to add meta data producer
   * @param thumbnailProducers the thumbnail producers from which to request the image before
   *   falling back to the full image producer sequence
   * @return local transformations sequence
   */
  private fun newLocalTransformationsSequence(
      inputProducer: Producer<EncodedImage>,
      thumbnailProducers: Array<ThumbnailProducer<EncodedImage>>
  ): Producer<EncodedImage> {
    var localImageProducer: Producer<EncodedImage> =
        ProducerFactory.newAddImageTransformMetaDataProducer(inputProducer)
    localImageProducer =
        producerFactory.newResizeAndRotateProducer(localImageProducer, true, imageTranscoderFactory)
    val localImageThrottlingProducer = producerFactory.newThrottlingProducer(localImageProducer)
    return ProducerFactory.newBranchOnSeparateImagesProducer(
        newLocalThumbnailProducer(thumbnailProducers), localImageThrottlingProducer)
  }

  private fun newLocalThumbnailProducer(
      thumbnailProducers: Array<ThumbnailProducer<EncodedImage>>
  ): Producer<EncodedImage> {
    val thumbnailBranchProducer = producerFactory.newThumbnailBranchProducer(thumbnailProducers)
    return producerFactory.newResizeAndRotateProducer(
        thumbnailBranchProducer, true, imageTranscoderFactory)
  }

  /** post-processor producer -> copy producer -> inputProducer */
  @Synchronized
  private fun getPostprocessorSequence(
      inputProducer: Producer<CloseableReference<CloseableImage>>
  ): Producer<CloseableReference<CloseableImage>> {
    var result = postprocessorSequences[inputProducer]
    if (result == null) {
      val postprocessorProducer = producerFactory.newPostprocessorProducer(inputProducer)
      result = producerFactory.newPostprocessorBitmapMemoryCacheProducer(postprocessorProducer)
      postprocessorSequences[inputProducer] = result
    }
    return result
  }

  /** swallow result producer -> inputProducer */
  @Synchronized
  private fun getDecodedImagePrefetchSequence(
      inputProducer: Producer<CloseableReference<CloseableImage>>
  ): Producer<Void?> {
    var result = closeableImagePrefetchSequences[inputProducer]
    if (result == null) {
      result = producerFactory.newSwallowResultProducer(inputProducer)
      closeableImagePrefetchSequences[inputProducer] = result
    }
    return result
  }

  /** bitmap prepare producer -> inputProducer */
  @Synchronized
  private fun getBitmapPrepareSequence(
      inputProducer: Producer<CloseableReference<CloseableImage>>
  ): Producer<CloseableReference<CloseableImage>> {
    var bitmapPrepareProducer = bitmapPrepareSequences[inputProducer]
    if (bitmapPrepareProducer == null) {
      bitmapPrepareProducer = producerFactory.newBitmapPrepareProducer(inputProducer)
      bitmapPrepareSequences[inputProducer] = bitmapPrepareProducer
    }
    return bitmapPrepareProducer
  }

  @Synchronized
  private fun getDelaySequence(
      inputProducer: Producer<CloseableReference<CloseableImage>>
  ): Producer<CloseableReference<CloseableImage>> {
    return producerFactory.newDelayProducer(inputProducer)
  }

  companion object {
    private fun validateEncodedImageRequest(imageRequest: ImageRequest) {
      Preconditions.checkArgument(
          imageRequest.lowestPermittedRequestLevel.value <=
              ImageRequest.RequestLevel.ENCODED_MEMORY_CACHE.value)
    }

    private fun getShortenedUriString(uri: Uri): String {
      val uriString = uri.toString()
      return if (uriString.length > 30) uriString.substring(0, 30) + "..." else uriString
    }
  }
}
