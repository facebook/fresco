/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import java.util.concurrent.Executor

class ExperimentalThreadHandoffProducerQueueImpl(executor: Executor?) : ThreadHandoffProducerQueue {

  private val executor: Executor = checkNotNull(executor)

  override fun addToQueueOrExecute(runnable: Runnable) {
    this.executor.execute(runnable)
  }

  override fun startQueueing() {
    throw UnsupportedOperationException()
  }

  override fun stopQueuing() {
    throw UnsupportedOperationException()
  }

  override fun remove(runnable: Runnable) {
    // NOOP
  }

  override fun isQueueing(): Boolean = false
}
