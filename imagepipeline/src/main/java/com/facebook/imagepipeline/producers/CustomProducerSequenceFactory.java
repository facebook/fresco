/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.references.CloseableReference;
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
}
