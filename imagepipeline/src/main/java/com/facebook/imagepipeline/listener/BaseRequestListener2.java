/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener;

import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class BaseRequestListener2 implements RequestListener2 {

  @Override
  public void onRequestStart(ProducerContext producerContext) {}

  @Override
  public void onRequestSuccess(ProducerContext producerContext) {}

  @Override
  public void onRequestFailure(ProducerContext producerContext, @Nullable Throwable throwable) {}

  @Override
  public void onRequestCancellation(ProducerContext producerContext) {}

  @Override
  public void onProducerStart(ProducerContext producerContext, String producerName) {}

  @Override
  public void onProducerEvent(
      ProducerContext producerContext, String producerName, String eventName) {}

  @Override
  public void onProducerFinishWithSuccess(
      ProducerContext producerContext,
      String producerName,
      @Nullable Map<String, String> extraMap) {}

  @Override
  public void onProducerFinishWithFailure(
      ProducerContext producerContext,
      String producerName,
      @Nullable Throwable t,
      @Nullable Map<String, String> extraMap) {}

  @Override
  public void onProducerFinishWithCancellation(
      ProducerContext producerContext,
      String producerName,
      @Nullable Map<String, String> extraMap) {}

  @Override
  public void onUltimateProducerReached(
      ProducerContext producerContext, String producerName, boolean successful) {}

  @Override
  public boolean requiresExtraMap(ProducerContext producerContext, String producerName) {
    return false;
  }
}
