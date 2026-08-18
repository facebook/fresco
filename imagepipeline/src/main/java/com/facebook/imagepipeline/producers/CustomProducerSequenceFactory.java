/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.core.ProducerFactory;
import com.facebook.imagepipeline.core.ProducerSequenceFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.OkToExtend;
import javax.annotation.Nullable;

@OkToExtend
@Nullsafe(Nullsafe.Mode.LOCAL)
public class CustomProducerSequenceFactory {

  public CustomProducerSequenceFactory() {}

  public @Nullable Producer<CloseableReference<CloseableImage>> getCustomDecodedImageSequence(
      ImageRequest imageRequest, ProducerSequenceFactory producerSequenceFactory) {
    return null;
  }

  public @Nullable Producer<CloseableReference<CloseableImage>> getCustomDecodedImageSequence(
      ImageRequest imageRequest,
      ProducerSequenceFactory producerSequenceFactory,
      ProducerFactory producerFactory,
      ThreadHandoffProducerQueue threadHandoffProducerQueue,
      boolean isEncodedMemoryCacheProbingEnabled,
      boolean isDiskCacheProbingEnabled) {
    return getCustomDecodedImageSequence(imageRequest, producerSequenceFactory);
  }

  public @Nullable Producer<CloseableReference<PooledByteBuffer>> getCustomEncodedImageSequence(
      ImageRequest imageRequest,
      ProducerSequenceFactory producerSequenceFactory,
      ProducerFactory producerFactory,
      ThreadHandoffProducerQueue threadHandoffProducerQueue) {
    return null;
  }

  /**
   * Claims a request whose URI is a network URI, i.e. one that would otherwise be served by the
   * standard network fetch sequence.
   *
   * <p>Unlike {@link #getCustomDecodedImageSequence}, which is only consulted for URI schemes the
   * pipeline does not recognise, this is consulted for {@code http(s)} requests. It is therefore
   * only called when {@code ImagePipelineExperiments#getAllowCustomNetworkSequences} returns true,
   * so that a config which does not opt in keeps the standard dispatch untouched.
   *
   * <p>Returning null means "not mine" and the standard network fetch sequence is used.
   */
  public @Nullable Producer<CloseableReference<CloseableImage>>
      getCustomNetworkDecodedImageSequence(
          ImageRequest imageRequest,
          ProducerSequenceFactory producerSequenceFactory,
          ProducerFactory producerFactory,
          ThreadHandoffProducerQueue threadHandoffProducerQueue,
          boolean isEncodedMemoryCacheProbingEnabled,
          boolean isDiskCacheProbingEnabled) {
    return null;
  }
}
