/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

import android.os.Process
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/** ThreadFactory that applies a priority to the threads it creates. */
class PriorityThreadFactory
/**
 * Creates a new PriorityThreadFactory with a given priority.
 *
 * This value should be set to a value compatible with [android.os.Process#setThreadPriority], not
 * [Thread#setPriority].
 */
@JvmOverloads
constructor(
    private val threadPriority: Int,
    private val prefix: String = "PriorityThreadFactory",
    private val addThreadNumber: Boolean = true
) : ThreadFactory {
  private val threadNumber = AtomicInteger(1)

  override fun newThread(runnable: Runnable): Thread {
    val wrapperRunnable = Runnable {
      try {
        Process.setThreadPriority(threadPriority)
      } catch (t: Throwable) {
        // just to be safe
      }
      runnable.run()
    }
    val name: String =
        if (addThreadNumber) {
          "${prefix}-${threadNumber.getAndIncrement()}"
        } else {
          prefix
        }
    return Thread(wrapperRunnable, name)
  }
}
