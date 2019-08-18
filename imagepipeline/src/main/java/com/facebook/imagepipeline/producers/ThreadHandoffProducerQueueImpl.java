/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

public class ThreadHandoffProducerQueueImpl implements ThreadHandoffProducerQueue {
  private boolean mQueueing = false;
  private final Deque<Runnable> mRunnableList;
  private final Executor mExecutor;

  public ThreadHandoffProducerQueueImpl(Executor executor) {
    mExecutor = Preconditions.checkNotNull(executor);
    mRunnableList = new ArrayDeque<>();
  }

  @Override
  public synchronized void addToQueueOrExecute(Runnable runnable) {
    if (mQueueing) {
      mRunnableList.add(runnable);
    } else {
      mExecutor.execute(runnable);
    }
  }

  @Override
  public synchronized void startQueueing() {
    mQueueing = true;
  }

  @Override
  public synchronized void stopQueuing() {
    mQueueing = false;
    execInQueue();
  }

  private void execInQueue() {
    while (!mRunnableList.isEmpty()) {
      mExecutor.execute(mRunnableList.pop());
    }
    mRunnableList.clear();
  }

  @Override
  public synchronized void remove(Runnable runnable) {
    mRunnableList.remove(runnable);
  }

  @Override
  public synchronized boolean isQueueing() {
    return mQueueing;
  }
}
