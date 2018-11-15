/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.executors.StatefulRunnable;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * {@link StatefulRunnable} intended to be used by producers.
 *
 * <p> Class implements common functionality related to handling producer instrumentation and
 * resource management.
 */
public abstract class StatefulProducerRunnable<T>
    extends StatefulRunnable<T> {

  private final Consumer<T> mConsumer;
  private final ProducerListener mProducerListener;
  private final String mProducerName;
  private final String mRequestId;

  public StatefulProducerRunnable(
      Consumer<T> consumer,
      ProducerListener producerListener,
      String producerName,
      String requestId) {
    mConsumer = consumer;
    mProducerListener = producerListener;
    mProducerName = producerName;
    mRequestId = requestId;

    mProducerListener.onProducerStart(mRequestId, mProducerName);
  }

  @Override
  protected void onSuccess(T result) {
    mProducerListener.onProducerFinishWithSuccess(
        mRequestId,
        mProducerName,
        mProducerListener.requiresExtraMap(mRequestId) ? getExtraMapOnSuccess(result) : null);
    mConsumer.onNewResult(result, Consumer.IS_LAST);
  }

  @Override
  protected void onFailure(Exception e) {
    mProducerListener.onProducerFinishWithFailure(
        mRequestId,
        mProducerName,
        e,
        mProducerListener.requiresExtraMap(mRequestId) ? getExtraMapOnFailure(e) : null);
    mConsumer.onFailure(e);
  }

  @Override
  protected void onCancellation() {
    mProducerListener.onProducerFinishWithCancellation(
        mRequestId,
        mProducerName,
        mProducerListener.requiresExtraMap(mRequestId) ? getExtraMapOnCancellation() : null);
    mConsumer.onCancellation();
  }

  /** Create extra map for result */
  protected @Nullable Map<String, String> getExtraMapOnSuccess(T result) {
    return null;
  }

  /** Create extra map for exception */
  protected @Nullable Map<String, String> getExtraMapOnFailure(Exception exception) {
    return null;
  }

  /** Create extra map for cancellation */
  protected @Nullable Map<String, String> getExtraMapOnCancellation() {
    return null;
  }

  @Override
  protected abstract void disposeResult(T result);
}
