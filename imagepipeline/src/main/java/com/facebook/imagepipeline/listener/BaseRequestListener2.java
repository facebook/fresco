/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.listener;

import androidx.annotation.NonNull;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class BaseRequestListener2 implements RequestListener2 {

  @Override
  public void onRequestStart(@NonNull ProducerContext producerContext) {}

  @Override
  public void onRequestSuccess(@NonNull ProducerContext producerContext) {}

  @Override
  public void onRequestFailure(
      @NonNull ProducerContext producerContext, @Nullable Throwable throwable) {}

  @Override
  public void onRequestCancellation(@NonNull ProducerContext producerContext) {}

  @Override
  public void onProducerStart(
      @NonNull ProducerContext producerContext, @NonNull String producerName) {}

  @Override
  public void onProducerEvent(
      @NonNull ProducerContext producerContext,
      @NonNull String producerName,
      @NonNull String eventName) {}

  @Override
  public void onProducerFinishWithSuccess(
      @NonNull ProducerContext producerContext,
      @NonNull String producerName,
      @Nullable Map<String, String> extraMap) {}

  @Override
  public void onProducerFinishWithFailure(
      @NonNull ProducerContext producerContext,
      String producerName,
      @Nullable Throwable t,
      @Nullable Map<String, String> extraMap) {}

  @Override
  public void onProducerFinishWithCancellation(
      @NonNull ProducerContext producerContext,
      @NonNull String producerName,
      @Nullable Map<String, String> extraMap) {}

  @Override
  public void onUltimateProducerReached(
      @NonNull ProducerContext producerContext, @NonNull String producerName, boolean successful) {}

  @Override
  public boolean requiresExtraMap(
      @NonNull ProducerContext producerContext, @NonNull String producerName) {
    return false;
  }
}
