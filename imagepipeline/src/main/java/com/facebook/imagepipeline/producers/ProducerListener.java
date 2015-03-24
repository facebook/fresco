/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;

import java.util.Map;

/**
 * Instrumentation for Producers.
 *
 * <p> Implementation of a producer should call these methods when appropriate to notify other
 * components interested in execution progress.
 */
public interface ProducerListener {

  /**
   * Called whenever a producer starts processing unit of work. This method might be called multiple
   * times, but between any two consecutive calls to onProducerStart onProducerFinishWithSuccess
   * will be called exactly once.
   */
  void onProducerStart(String requestId, String producerName);

  /**
   * Called whenever an important producer-specific event occurs. This may only be called if
   * onProducerStart has been called, but corresponding onProducerFinishWith* method has not been
   * called yet.
   */
  void onProducerEvent(String requestId, String producerName, String eventName);

  /**
   * Called when a producer successfully finishes processing current unit of work.
   *
   * @param extraMap Additional parameters about the producer. This map is immutable and will
   * throw an exception if attempts are made to modify it.
   */
  void onProducerFinishWithSuccess(
      String requestId,
      String producerName,
      @Nullable Map<String, String> extraMap);

  /**
   * Called when producer finishes processing current unit of work due to an error.
   *
   * @param extraMap Additional parameters about the producer. This map is immutable and will
   * throw an exception if attempts are made to modify it.
   */
  void onProducerFinishWithFailure(
      String requestId,
      String producerName,
      Throwable t,
      @Nullable Map<String, String> extraMap);

  /**
   * Called once when producer finishes due to cancellation.
   *
   * @param extraMap Additional parameters about the producer. This map is immutable and will
   * throw an exception if attempts are made to modify it.
   */
  void onProducerFinishWithCancellation(
      String requestId,
      String producerName,
      @Nullable Map<String, String> extraMap);

  /**
   * @return true if listener makes use of extra map
   * @param requestId
   */
  boolean requiresExtraMap(String requestId);
}
