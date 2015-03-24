/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

/**
 * Producer that never produces anything, but just returns null.
 *
 * <p>This producer can be used to terminate a sequence, e.g. for a bitmap cache get only sequence,
 * just use BitmapMemoryCacheGetProducer followed by NullProducer.
 */
public class NullProducer<T> implements Producer<T> {

  /**
   * Start producing results for given context. Provided consumer is notified whenever progress is
   * made (new value is ready or error occurs).
   * @param consumer
   * @param context
   */
  public void produceResults(Consumer<T> consumer, ProducerContext context) {
    consumer.onNewResult((T) null, true);
  }
}
