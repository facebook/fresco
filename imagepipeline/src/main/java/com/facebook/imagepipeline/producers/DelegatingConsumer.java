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
 * Delegating consumer.
 */
public abstract class DelegatingConsumer<I, O> extends BaseConsumer<I> {

  private final Consumer<O> mConsumer;

  public DelegatingConsumer(Consumer<O> consumer) {
    mConsumer = consumer;
  }

  public Consumer<O> getConsumer() {
    return mConsumer;
  }

  @Override
  protected void onFailureImpl(Throwable t) {
    mConsumer.onFailure(t);
  }

  @Override
  protected void onCancellationImpl() {
    mConsumer.onCancellation();
  }

  @Override
  protected void onProgressUpdateImpl(float progress) {
    mConsumer.onProgressUpdate(progress);
  }
}
