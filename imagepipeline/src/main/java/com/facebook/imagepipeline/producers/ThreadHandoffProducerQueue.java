/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.internal.Preconditions;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class ThreadHandoffProducerQueue {
  private boolean mQueueing = false;
  private final ArrayList<Runnable> mRunnableList;
  private final Executor mExecutor;

  public ThreadHandoffProducerQueue(Executor executor) {
    mExecutor = Preconditions.checkNotNull(executor);
    mRunnableList = new ArrayList<>();
  }

  public synchronized void addToQueueOrExecute(Runnable runnable) {
    if (mQueueing) {
      mRunnableList.add(runnable);
    } else {
      mExecutor.execute(runnable);
    }
  }

  public synchronized void startQueueing() {
    mQueueing = true;
  }

  public synchronized void stopQueuing() {
    mQueueing = false;
    execInQueue();
  }

  private void execInQueue() {
    for (Runnable runnable : mRunnableList) {
      mExecutor.execute(runnable);
    }
    mRunnableList.clear();
  }

  public void remove(Runnable runnable) {
    mRunnableList.remove(runnable);
  }

  public synchronized boolean isQueueing() {
    return mQueueing;
  }
}
