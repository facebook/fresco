/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.common.ResizeOptions;

/**
 * Implemented producers can be queried for whether they are likely to be able to produce a result
 * of the desired size.
 *
 * <p> {@link #produceResults(Consumer, ProducerContext)} may send a null image to the consumer,
 * even if an image is available, if the ultimate image is smaller than wanted. This may happen even
 * if the producer thought it would be able to satisfy the request.
 */
public interface ThumbnailProducer<T> extends Producer<T> {

  /**
   * Checks whether the producer may be able to produce images of the specified size. This makes no
   * promise about being able to produce images for a particular source, only generally being able
   * to produce output of the desired resolution.
   *
   * @param resizeOptions the resize options from the current request
   * @return true if the producer can meet these needs
   */
  boolean canProvideImageForSize(ResizeOptions resizeOptions);
}
