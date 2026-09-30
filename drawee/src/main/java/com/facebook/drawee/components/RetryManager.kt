/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components

/** Manages retries for an image. */
class RetryManager {

  var isTapToRetryEnabled: Boolean = false

  // automatically generated for Java compatibility, please inline it as soon as possible
  fun setIsTapToRetryEnabled(value: Boolean) {
    this.isTapToRetryEnabled = value
  }

  private var maxTapToRetryAttempts = 0
  private var tapToRetryAttempts = 0

  init {
    init()
  }

  /** Initializes component to its initial state. */
  fun init() {
    isTapToRetryEnabled = false
    maxTapToRetryAttempts = RetryManager.MAX_TAP_TO_RETRY_ATTEMPTS
    reset()
  }

  /** Resets component. This will reset the number of attempts. */
  fun reset() {
    tapToRetryAttempts = 0
  }

  fun setMaxTapToRetryAttemps(maxTapToRetryAttemps: Int) {
    this.maxTapToRetryAttempts = maxTapToRetryAttemps
  }

  fun shouldRetryOnTap(): Boolean =
      isTapToRetryEnabled && tapToRetryAttempts < maxTapToRetryAttempts

  fun notifyTapToRetry() {
    tapToRetryAttempts++
  }

  companion object {
    private const val MAX_TAP_TO_RETRY_ATTEMPTS = 4

    @JvmStatic fun newInstance(): RetryManager = RetryManager()
  }
}
