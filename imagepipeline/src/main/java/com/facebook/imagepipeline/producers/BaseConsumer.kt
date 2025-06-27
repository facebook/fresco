/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.producers

import android.annotation.SuppressLint
import com.facebook.common.logging.FLog
import javax.annotation.concurrent.ThreadSafe

/**
 * Base implementation of Consumer that implements error handling conforming to the Consumer's
 * contract.
 *
 * This class also prevents execution of callbacks if one of final methods was called before:
 * onFinish(isLast = true), onFailure or onCancellation.
 *
 * All callbacks are executed within a synchronized block, so that clients can act as if all
 * callbacks are called on single thread.
 *
 * @param <T> </T>
 */
@ThreadSafe
abstract class BaseConsumer<T> : Consumer<T> {
  /**
   * Set to true when onNewResult(isLast = true), onFailure or onCancellation is called. Further
   * calls to any of the 3 methods are not propagated
   */
  private var isFinished = false

  @Synchronized
  override fun onNewResult(newResult: T?, @Consumer.Status status: Int) {
    if (isFinished) {
      return
    }
    isFinished = isLast(status)
    try {
      onNewResultImpl(newResult, status)
    } catch (e: Exception) {
      onUnhandledException(e)
    }
  }

  @Synchronized
  override fun onFailure(t: Throwable?) {
    if (isFinished) {
      return
    }
    isFinished = true
    try {
      onFailureImpl(t ?: Throwable("null throwable"))
    } catch (e: Exception) {
      onUnhandledException(e)
    }
  }

  @Synchronized
  override fun onCancellation() {
    if (isFinished) {
      return
    }
    isFinished = true
    try {
      onCancellationImpl()
    } catch (e: Exception) {
      onUnhandledException(e)
    }
  }

  /**
   * Called when the progress updates.
   *
   * @param progress in range [0, 1]
   */
  @Synchronized
  override fun onProgressUpdate(progress: Float) {
    if (isFinished) {
      return
    }
    try {
      onProgressUpdateImpl(progress)
    } catch (e: Exception) {
      onUnhandledException(e)
    }
  }

  /** Called by onNewResult, override this method instead. */
  protected abstract fun onNewResultImpl(newResult: T?, @Consumer.Status status: Int)

  /** Called by onFailure, override this method instead */
  protected abstract fun onFailureImpl(t: Throwable)

  /** Called by onCancellation, override this method instead */
  protected abstract fun onCancellationImpl()

  /** Called when the progress updates */
  protected open fun onProgressUpdateImpl(progress: Float) {}

  /** Called whenever onNewResultImpl or onFailureImpl throw an exception */
  protected fun onUnhandledException(e: Exception?) {
    FLog.wtf(this.javaClass, "unhandled exception", e)
  }

  companion object {
    /**
     * Checks whether the provided status includes the `IS_LAST` flag, marking this as the last
     * result the consumer will receive.
     */
    @JvmStatic
    fun isLast(@Consumer.Status status: Int): Boolean {
      return (status and Consumer.IS_LAST) == Consumer.IS_LAST
    }

    /**
     * Checks whether the provided status includes the `IS_LAST` flag, marking this as the last
     * result the consumer will receive.
     */
    @JvmStatic
    fun isNotLast(@Consumer.Status status: Int): Boolean {
      return !isLast(status)
    }

    /** Updates a provided status by ensuring the specified flag is turned on. */
    @JvmStatic
    @Consumer.Status
    fun turnOnStatusFlag(@Consumer.Status status: Int, @Consumer.Status flag: Int): Int {
      return status or flag
    }

    /** Updates a provided status by ensuring the specified flag is turned off. */
    @JvmStatic
    @Consumer.Status
    fun turnOffStatusFlag(@Consumer.Status status: Int, @Consumer.Status flag: Int): Int {
      return status and flag.inv()
    }

    /** Checks whether the provided status contains a specified flag. */
    @JvmStatic
    fun statusHasFlag(@Consumer.Status status: Int, @Consumer.Status flag: Int): Boolean {
      return (status and flag) == flag
    }

    /** Checks whether the provided status contains any of the specified flags. */
    @JvmStatic
    fun statusHasAnyFlag(@Consumer.Status status: Int, @Consumer.Status flag: Int): Boolean {
      return (status and flag) != 0
    }

    /** Creates a simple status value which only identifies whether this is the last result. */
    @JvmStatic
    @SuppressLint("WrongConstant")
    @Consumer.Status
    fun simpleStatusForIsLast(isLast: Boolean): Int {
      return if (isLast) Consumer.IS_LAST else Consumer.NO_FLAGS
    }
  }
}
