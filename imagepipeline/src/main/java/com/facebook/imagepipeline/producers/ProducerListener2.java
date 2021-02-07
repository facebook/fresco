/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import androidx.annotation.NonNull;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Instrumentation for Producers.
 *
 * <p>Producer implementations should call these methods when appropriate to notify other components
 * interested in the execution progress.
 */
public interface ProducerListener2 {

  /**
   * Called whenever a producer starts processing unit of work. This method might be called multiple
   * times, but between any two consecutive calls to onProducerStart onProducerFinishWithSuccess
   * will be called exactly once.
   */
  void onProducerStart(@NonNull ProducerContext producerContext, @NonNull String producerName);

  /**
   * Called whenever an important producer-specific event occurs. This may only be called if
   * onProducerStart has been called, but corresponding onProducerFinishWith* method has not been
   * called yet.
   */
  void onProducerEvent(
      @NonNull ProducerContext producerContext,
      @NonNull String producerName,
      @NonNull String eventName);

  /**
   * Called when a producer successfully finishes processing current unit of work.
   *
   * @param extraMap Additional parameters about the producer. This map is immutable and will throw
   *     an exception if attempts are made to modify it.
   */
  void onProducerFinishWithSuccess(
      @NonNull ProducerContext producerContext,
      @NonNull String producerName,
      @Nullable Map<String, String> extraMap);

  /**
   * Called when producer finishes processing current unit of work due to an error.
   *
   * @param extraMap Additional parameters about the producer. This map is immutable and will throw
   *     an exception if attempts are made to modify it.
   */
  void onProducerFinishWithFailure(
      @NonNull ProducerContext producerContext,
      String producerName,
      Throwable t,
      @Nullable Map<String, String> extraMap);

  /**
   * Called once when producer finishes due to cancellation.
   *
   * @param extraMap Additional parameters about the producer. This map is immutable and will throw
   *     an exception if attempts are made to modify it.
   */
  void onProducerFinishWithCancellation(
      @NonNull ProducerContext producerContext,
      @NonNull String producerName,
      @Nullable Map<String, String> extraMap);

  /**
   * Called when the producer which can create the final result for a given request has completed.
   *
   * <p>This can be used to determine which producer was best able to satisfy the request.
   */
  void onUltimateProducerReached(
      @NonNull ProducerContext producerContext, @NonNull String producerName, boolean successful);

  /** @return true if listener makes use of extra map */
  boolean requiresExtraMap(@NonNull ProducerContext producerContext, @NonNull String producerName);
}
