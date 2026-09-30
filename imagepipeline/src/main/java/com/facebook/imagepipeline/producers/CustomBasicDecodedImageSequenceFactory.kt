/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.core.ProducerFactory
import com.facebook.imagepipeline.core.ProducerSequenceFactory
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest

/**
 * Chooses the basic decoded image sequence for every request, in place of
 * [ProducerSequenceFactory]'s own dispatch on the source URI type.
 *
 * "Basic" means the sequence before the stages the pipeline appends itself: postprocessing, bitmap
 * prepare and delay are still added on top of whatever this returns.
 *
 * To keep the standard behaviour for a request, return
 * [ProducerSequenceFactory.getDefaultBasicDecodedImageSequence]. The result is used as a map key
 * for the appended stages, so return the same producer instance for requests that should share them
 * rather than a new one per request.
 */
fun interface CustomBasicDecodedImageSequenceFactory {
  fun getBasicDecodedImageSequence(
      imageRequest: ImageRequest,
      producerSequenceFactory: ProducerSequenceFactory,
      producerFactory: ProducerFactory,
      threadHandoffProducerQueue: ThreadHandoffProducerQueue,
  ): Producer<CloseableReference<CloseableImage>>
}
