/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.loadframe

import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * This executor allocated a high priority pool of thread. The priority is high because the task
 * affect directly to the UI render frame. In order to avoid glitches or bad UX, this tasks should
 * be finished asap
 */
object AnimationLoaderExecutor {
  private val THREADS = Runtime.getRuntime().availableProcessors() * 2

  private val uiThreadFactory = ThreadFactory { runnable: Runnable? ->
    val thread = Thread(runnable)
    thread.priority = Thread.MAX_PRIORITY - 1
    thread
  }

  private val executor: ThreadPoolExecutor =
      ThreadPoolExecutor(
          THREADS, THREADS, 0L, TimeUnit.MILLISECONDS, PriorityBlockingQueue(), uiThreadFactory)

  fun execute(task: LoadFramePriorityTask) {
    executor.execute(task)
  }
}

interface LoadFramePriorityTask : Comparable<LoadFramePriorityTask>, Runnable {
  val priority: Priority

  override fun compareTo(other: LoadFramePriorityTask): Int {
    return other.priority.compareTo(priority)
  }

  enum class Priority(val value: Int) {
    HIGH(Thread.MAX_PRIORITY),
    MEDIUM(Thread.NORM_PRIORITY),
    LOW(Thread.MIN_PRIORITY)
  }
}
