/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.executors;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstraction for computation.
 *
 * <p> Computation expressed as StatefulRunnable can be cancelled, but only if it has not
 * started yet.
 *
 * <p> For better decoupling of the code computing the result and the code that handles it, 4
 * separate methods are provided: getResult, onSuccess, onFailure and onCancellation.
 *
 * <p> This runnable can be run only once. Subsequent calls to run method won't have any effect.
 */
abstract public class StatefulRunnable<T> implements Runnable {
  protected static final int STATE_CREATED = 0;
  protected static final int STATE_STARTED = 1;
  protected static final int STATE_CANCELLED = 2;
  protected static final int STATE_FINISHED = 3;
  protected static final int STATE_FAILED = 4;

  protected final AtomicInteger mState;

  public StatefulRunnable() {
    mState = new AtomicInteger(STATE_CREATED);
  }

  @Override
  public final void run() {
    if (!mState.compareAndSet(STATE_CREATED, STATE_STARTED)) {
      return;
    }
    T result;
    try {
      result = getResult();
    } catch (Exception e) {
      mState.set(STATE_FAILED);
      onFailure(e);
      return;
    }

    mState.set(STATE_FINISHED);
    try {
      onSuccess(result);
    } finally {
      disposeResult(result);
    }
  }

  public void cancel() {
    if (mState.compareAndSet(STATE_CREATED, STATE_CANCELLED)) {
      onCancellation();
    }
  }

  /**
   * Called after computing result successfully.
   * @param result
   */
  protected void onSuccess(T result) {}

  /**
   * Called if exception occurred during computation.
   * @param e
   */
  protected void onFailure(Exception e) {}

  /**
   * Called when the runnable is cancelled.
   */
  protected void onCancellation() {}

  /**
   * Called after onSuccess callback completes in order to dispose the result.
   * @param result
   */
  protected void disposeResult(T result) {}

  abstract protected T getResult() throws Exception;
}
