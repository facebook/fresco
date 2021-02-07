/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.instrumentation;

/** Collects wait times and holds image loading stats. */
public class PerfListener {
  private long mSumOfWaitTime;
  private long mStartedRequests;
  private long mSuccessfulRequests;
  private long mCancelledRequests;
  private long mFailedRequests;

  public PerfListener() {
    mSumOfWaitTime = 0;
    mStartedRequests = 0;
    mSuccessfulRequests = 0;
    mCancelledRequests = 0;
    mFailedRequests = 0;
  }

  /** Called whenever image request finishes successfully, that is whenever final image is set. */
  public void reportSuccess(long waitTime) {
    mSumOfWaitTime += waitTime;
    mSuccessfulRequests++;
  }

  /** Called whenever image request fails, that is whenever failure drawable is set. */
  public void reportFailure(long waitTime) {
    mSumOfWaitTime += waitTime;
    mFailedRequests++;
  }

  /**
   * Called whenever image request is cancelled, that is whenever image view is reused without
   * setting final image first
   */
  public void reportCancellation(long waitTime) {
    mSumOfWaitTime += waitTime;
    mCancelledRequests++;
  }

  /** Called whenver new request is started. */
  public void reportStart() {
    mStartedRequests++;
  }

  /**
   * @return average wait time, that is sum of reported wait times divided by number of completed
   *     requests
   */
  public long getAverageWaitTime() {
    final long completedRequests = getCompletedRequests();
    return completedRequests > 0 ? mSumOfWaitTime / completedRequests : 0;
  }

  /** @return difference between number of started requests and number of completed requests */
  public long getOutstandingRequests() {
    return mStartedRequests - getCompletedRequests();
  }

  /** @return number of cancelled requests */
  public long getCancelledRequests() {
    return mCancelledRequests;
  }

  /** @return number of completed requests, either by seting final image, failure or cancellation */
  public long getCompletedRequests() {
    return mSuccessfulRequests + mCancelledRequests + mFailedRequests;
  }
}
