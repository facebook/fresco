/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation.loadframe

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object AnimationLoaderExecutor {

  private val frameThreadFactory = ThreadFactory { runnable: Runnable? ->
    val thread = Thread(runnable)
    thread.priority = Thread.MIN_PRIORITY
    thread
  }

  private val executor = Executors.newCachedThreadPool(frameThreadFactory)

  fun execute(task: Runnable) {
    executor.execute(task)
  }
}
