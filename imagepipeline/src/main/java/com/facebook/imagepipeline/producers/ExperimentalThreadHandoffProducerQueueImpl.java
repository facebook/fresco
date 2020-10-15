/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Executor;

@Nullsafe(Nullsafe.Mode.STRICT)
public class ExperimentalThreadHandoffProducerQueueImpl implements ThreadHandoffProducerQueue {
  private final Executor mExecutor;

  public ExperimentalThreadHandoffProducerQueueImpl(Executor executor) {
    mExecutor = Preconditions.checkNotNull(executor);
  }

  @Override
  public void addToQueueOrExecute(Runnable runnable) {
    mExecutor.execute(runnable);
  }

  @Override
  public void startQueueing() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void stopQueuing() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(Runnable runnable) {
    // NOOP
  }

  @Override
  public boolean isQueueing() {
    return false;
  }
}
