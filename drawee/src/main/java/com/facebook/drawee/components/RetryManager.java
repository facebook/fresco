/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.components;

/**
 * Manages retries for an image.
 */
public class RetryManager {
  private static final int MAX_TAP_TO_RETRY_ATTEMPTS = 4;

  private boolean mTapToRetryEnabled;
  private int mMaxTapToRetryAttempts;
  private int mTapToRetryAttempts;

  public RetryManager() {
    init();
  }

  public static RetryManager newInstance() {
    return new RetryManager();
  }

  /**
   * Initializes component to its initial state.
   */
  public void init() {
    mTapToRetryEnabled = false;
    mMaxTapToRetryAttempts = MAX_TAP_TO_RETRY_ATTEMPTS;
    reset();
  }

  /**
   * Resets component.
   * This will reset the number of attempts.
   */
  public void reset() {
    mTapToRetryAttempts = 0;
  }

  public boolean isTapToRetryEnabled() {
    return mTapToRetryEnabled;
  }

  public void setTapToRetryEnabled(boolean tapToRetryEnabled) {
    mTapToRetryEnabled = tapToRetryEnabled;
  }

  public boolean shouldRetryOnTap() {
    return mTapToRetryEnabled && mTapToRetryAttempts < mMaxTapToRetryAttempts;
  }

  public void notifyTapToRetry() {
    mTapToRetryAttempts++;
  }
}
