/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

/**
 * A target that a [TickScheduler] can drive, one animation frame at a time.
 *
 * Implementations decide what a tick means for them; the scheduler only forwards the frame
 * timestamp it was given by the system.
 */
interface Tickable {

  /**
   * Called once per scheduled frame.
   *
   * @param frameTimeNanos timestamp of the frame being rendered, in nanoseconds
   * @return true if this target should keep receiving ticks, false if it is done
   */
  fun doFrame(frameTimeNanos: Long): Boolean
}
