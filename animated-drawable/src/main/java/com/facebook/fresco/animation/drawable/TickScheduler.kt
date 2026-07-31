/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.drawable

/**
 * Drives one or more [Tickable]s from a single source of animation frames.
 *
 * Deliberately named apart from [com.facebook.fresco.animation.frame.FrameScheduler], which holds
 * per-drawable timing state and is a different abstraction.
 */
interface TickScheduler {

  /** Starts delivering frames to [drawable]. */
  fun register(drawable: Tickable)

  /** Stops delivering frames to [drawable]. */
  fun unregister(drawable: Tickable)

  /** Whether [drawable] is currently being delivered frames. */
  fun isRegistered(drawable: Tickable): Boolean
}
