/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.producers.Producer
import com.facebook.imagepipeline.request.ImageRequest

/** Supplies the producer sequences used by [ImagePipeline]. */
interface ProducerSequenceFactoryInterface {
  fun getDecodedImageProducerSequence(
      imageRequest: ImageRequest,
  ): Producer<CloseableReference<CloseableImage>>

  fun getEncodedImageProducerSequence(
      imageRequest: ImageRequest,
  ): Producer<CloseableReference<PooledByteBuffer>>

  fun getDecodedImagePrefetchProducerSequence(imageRequest: ImageRequest): Producer<Void?>

  fun getEncodedImagePrefetchProducerSequence(imageRequest: ImageRequest): Producer<Void?>
}
