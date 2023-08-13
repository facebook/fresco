/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import java.util.ArrayDeque
import java.util.Deque
import java.util.concurrent.Executor

class ThreadHandoffProducerQueueImpl(private val executor: Executor) : ThreadHandoffProducerQueue {

  private var queueing = false
  private val runnableList: Deque<Runnable> = ArrayDeque()

  @Synchronized
  override fun addToQueueOrExecute(runnable: Runnable) {
    if (queueing) {
      runnableList.add(runnable)
    } else {
      this.executor.execute(runnable)
    }
  }

  @Synchronized
  override fun startQueueing() {
    queueing = true
  }

  @Synchronized
  override fun stopQueuing() {
    queueing = false
    execInQueue()
  }

  private fun execInQueue() {
    while (!runnableList.isEmpty()) {
      this.executor.execute(runnableList.pop())
    }
    runnableList.clear()
  }

  @Synchronized
  override fun remove(runnable: Runnable) {
    runnableList.remove(runnable)
  }

  @Synchronized override fun isQueueing(): Boolean = queueing
}
