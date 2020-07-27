/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import androidx.annotation.StringDef;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.image.EncodedImageOrigin;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Used to pass context information to producers.
 *
 * <p>Object implementing this interface is passed to all producers participating in pipeline
 * request {@see Producer#produceResults}. Its responsibility is to instruct producers which image
 * should be fetched/decoded/resized/cached etc. This class also handles request cancellation.
 *
 * <p>In order to be notified when cancellation is requested, a producer should use the {@code
 * runOnCancellationRequested} method which takes an instance of Runnable and executes it when the
 * pipeline client cancels the image request.
 */
public interface ProducerContext {

  @StringDef({
    ExtraKeys.ORIGIN,
    ExtraKeys.ORIGIN_SUBCATEGORY,
    ExtraKeys.NORMALIZED_URI,
    ExtraKeys.SOURCE_URI,
    ExtraKeys.IMAGE_FORMAT,
    ExtraKeys.ENCODED_WIDTH,
    ExtraKeys.ENCODED_HEIGHT,
    ExtraKeys.ENCODED_SIZE,
    ExtraKeys.MULTIPLEX_BITMAP_COUNT,
    ExtraKeys.MULTIPLEX_ENCODED_COUNT,
  })
  @interface ExtraKeys {
    String ORIGIN = "origin";
    String ORIGIN_SUBCATEGORY = "origin_sub";
    String SOURCE_URI = "uri_source";
    String NORMALIZED_URI = "uri_norm";
    String IMAGE_FORMAT = "image_format";
    String ENCODED_WIDTH = "encoded_width";
    String ENCODED_HEIGHT = "encoded_height";
    String ENCODED_SIZE = "encoded_size";
    /* number of deduped request in BitmapMemoryCacheKeyMultiplexProducer */
    String MULTIPLEX_BITMAP_COUNT = "multiplex_bmp_cnt";
    /* number of deduped request in EncodedCacheKeyMultiplexProducer */
    String MULTIPLEX_ENCODED_COUNT = "multiplex_enc_cnt";
  }

  /** @return image request that is being executed */
  ImageRequest getImageRequest();

  /** @return id of this request */
  String getId();

  /** @return optional id of the UI component requesting the image */
  @Nullable
  String getUiComponentId();

  /** @return ProducerListener2 for producer's events */
  ProducerListener2 getProducerListener();

  /** @return the {@link Object} that indicates the caller's context */
  Object getCallerContext();

  /** @return the lowest permitted {@link ImageRequest.RequestLevel} */
  ImageRequest.RequestLevel getLowestPermittedRequestLevel();

  /** @return true if the request is a prefetch, false otherwise. */
  boolean isPrefetch();

  /** @return priority of the request. */
  Priority getPriority();

  /** @return true if request's owner expects intermediate results */
  boolean isIntermediateResultExpected();

  /**
   * Adds callbacks to the set of callbacks that are executed at various points during the
   * processing of a request.
   *
   * @param callbacks callbacks to be executed
   */
  void addCallbacks(ProducerContextCallbacks callbacks);

  ImagePipelineConfig getImagePipelineConfig();

  EncodedImageOrigin getEncodedImageOrigin();

  void setEncodedImageOrigin(EncodedImageOrigin encodedImageOrigin);

  <E> void setExtra(String key, @Nullable E value);

  void putExtras(@Nullable Map<String, ?> extras);

  @Nullable
  <E> E getExtra(String key);

  @Nullable
  <E> E getExtra(String key, @Nullable E valueIfNotFound);

  Map<String, Object> getExtras();

  /** Helper to set {@link ExtraKeys#ORIGIN} and {@link ExtraKeys#ORIGIN_SUBCATEGORY} */
  void putOriginExtra(@Nullable String origin, @Nullable String subcategory);

  /** Helper to set {@link ExtraKeys#ORIGIN} */
  void putOriginExtra(@Nullable String origin);
}
