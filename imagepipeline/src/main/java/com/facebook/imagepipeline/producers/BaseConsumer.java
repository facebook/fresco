/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.logging.FLog;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Base implementation of Consumer that implements error handling conforming to the
 * Consumer's contract.
 *
 * <p> This class also prevents execution of callbacks if one of final methods was called before:
 * onFinish(isLast = true), onFailure or onCancellation.
 *
 * <p> All callbacks are executed within a synchronized block, so that clients can act as if all
 * callbacks are called on single thread.
 *
 * @param <T>
 */
@ThreadSafe
public abstract class BaseConsumer<T> implements Consumer<T> {

  /**
   * Set to true when onNewResult(isLast = true), onFailure or onCancellation is called. Further
   * calls to any of the 3 methods are not propagated
   */
  private boolean mIsFinished;

  public BaseConsumer() {
    mIsFinished = false;
  }

  /**
   * Checks whether the provided status includes the `IS_LAST` flag, marking this as the last result
   * the consumer will receive.
   */
  public static boolean isLast(@Consumer.Status int status) {
    return (status & Consumer.IS_LAST) == Consumer.IS_LAST;
  }

  /**
   * Checks whether the provided status includes the `IS_LAST` flag, marking this as the last result
   * the consumer will receive.
   */
  public static boolean isNotLast(@Consumer.Status int status) {
    return !isLast(status);
  }

  /**
   * Updates a provided status by ensuring the specified flag is turned on.
   */
  public static @Status int turnOnStatusFlag(@Status int status, @Status int flag) {
    return status | flag;
  }

  /**
   * Updates a provided status by ensuring the specified flag is turned off.
   */
  public static @Status int turnOffStatusFlag(@Status int status, @Status int flag) {
    return status & ~flag;
  }

  /**
   * Checks whether the provided status contains a specified flag.
   */
  public static boolean statusHasFlag(@Status int status, @Status int flag) {
    return (status & flag) == flag;
  }

  /**
   * Checks whether the provided status contains any of the specified flags.
   */
  public static boolean statusHasAnyFlag(@Status int status, @Status int flag) {
    return (status & flag) != 0;
  }

  /**
   * Creates a simple status value which only identifies whether this is the last result.
   */
  public static @Status int simpleStatusForIsLast(boolean isLast) {
    return isLast ? IS_LAST : NO_FLAGS;
  }

  @Override
  public synchronized void onNewResult(@Nullable T newResult, @Status int status) {
    if (mIsFinished) {
      return;
    }
    mIsFinished = isLast(status);
    try {
      onNewResultImpl(newResult, status);
    } catch (Exception e) {
      onUnhandledException(e);
    }
  }

  @Override
  public synchronized void onFailure(Throwable t) {
    if (mIsFinished) {
      return;
    }
    mIsFinished = true;
    try {
      onFailureImpl(t);
    } catch (Exception e) {
      onUnhandledException(e);
    }
  }

  @Override
  public synchronized void onCancellation() {
    if (mIsFinished) {
      return;
    }
    mIsFinished = true;
    try {
      onCancellationImpl();
    } catch (Exception e) {
      onUnhandledException(e);
    }
  }

  /**
   * Called when the progress updates.
   *
   * @param progress in range [0, 1]
   */
  @Override
  public synchronized void onProgressUpdate(float progress) {
    if (mIsFinished) {
      return;
    }
    try {
      onProgressUpdateImpl(progress);
    } catch (Exception e) {
      onUnhandledException(e);
    }
  }

  /**
   * Called by onNewResult, override this method instead.
   */
  protected abstract void onNewResultImpl(T newResult, @Status int status);

  /**
   * Called by onFailure, override this method instead
   */
  protected abstract void onFailureImpl(Throwable t);

  /**
   * Called by onCancellation, override this method instead
   */
  protected abstract void onCancellationImpl();

  /**
   * Called when the progress updates
   */
  protected void onProgressUpdateImpl(float progress) {
  }

  /**
   * Called whenever onNewResultImpl or onFailureImpl throw an exception
   */
  protected void onUnhandledException(Exception e) {
    FLog.wtf(this.getClass(), "unhandled exception", e);
  }
}
