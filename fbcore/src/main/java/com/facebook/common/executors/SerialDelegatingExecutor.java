/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.common.executors;

import javax.annotation.concurrent.GuardedBy;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * Simple implementation of delegating Executor that limits concurrency of execution to single
 * thread.
 */
public class SerialDelegatingExecutor implements Executor {

  private final Executor mDelegate;
  @VisibleForTesting
  final Runnable mRunnable;

  /**
   * True if and only if runnable has been passed to mDelegate for execution, but the execution
   * itself has not completed yet.
   */
  @GuardedBy("this")
  @VisibleForTesting
  boolean mExecutionInProgress;
  @GuardedBy("this")
  final private Queue<Runnable> mCommands;

  public SerialDelegatingExecutor(Executor delegate) {
    mDelegate = Preconditions.checkNotNull(delegate);
    mExecutionInProgress = false;
    mCommands = new LinkedList<Runnable>();
    mRunnable = new Runnable() {
      @Override
      public void run() {
        executeSingleCommand();
      }
    };
  }

  /**
   * Submits another command for execution
   */
  @Override
  public void execute(Runnable command) {
    synchronized (this) {
      mCommands.add(command);
    }
    maybeSubmitRunnable();
  }

  private void maybeSubmitRunnable() {
    synchronized (this) {
      if (mExecutionInProgress || mCommands.isEmpty()) {
        return;
      }
      mExecutionInProgress = true;
    }
    mDelegate.execute(mRunnable);
  }

  private void executeSingleCommand() {
    Runnable command;
    try {
      removeNextCommand().run();
    } finally {
      clearExecutionInProgress();
      maybeSubmitRunnable();
    }
  }

  private synchronized Runnable removeNextCommand() {
    return mCommands.remove();
  }

  private synchronized void clearExecutionInProgress() {
    mExecutionInProgress = false;
  }
}
