/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

/**
 * Building block for image processing in the image pipeline.
 *
 * <p> Execution of image request consists of multiple different tasks such as network fetch,
 * disk caching, memory caching, decoding, applying transformations etc. Producer<T> represents
 * single task whose result is an instance of T. Breaking entire request into sequence of
 * Producers allows us to construct different requests while reusing the same blocks.
 *
 * <p> Producer supports multiple values and streaming.
 *
 * @param <T>
 */
public interface Producer<T> {

  /**
   * Start producing results for given context. Provided consumer is notified whenever progress is
   * made (new value is ready or error occurs).
   * @param consumer
   * @param context
   */
  void produceResults(Consumer<T> consumer, ProducerContext context);
}
