/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener;

import androidx.annotation.NonNull;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.imagepipeline.producers.ProducerListener2;
import com.facebook.imagepipeline.request.ImageRequest;

/** Listener for {@link ImageRequest} with access to the full {@link ProducerContext}. */
public interface RequestListener2 extends ProducerListener2 {

  /**
   * Called when request is about to be submitted to the Orchestrator's executor queue.
   *
   * @param producerContext the producer context for the image
   */
  void onRequestStart(@NonNull ProducerContext producerContext);

  /**
   * Called after successful completion of the request (all producers completed successfully).
   *
   * @param producerContext the producer context for the image
   */
  void onRequestSuccess(@NonNull ProducerContext producerContext);

  /**
   * Called after failure to complete the request (some producer failed).
   *
   * @param producerContext the producer context for the image
   * @param throwable cause of failure
   */
  void onRequestFailure(@NonNull ProducerContext producerContext, Throwable throwable);

  /**
   * Called after the request is cancelled.
   *
   * @param producerContext the producer context for the image
   */
  void onRequestCancellation(@NonNull ProducerContext producerContext);
}
