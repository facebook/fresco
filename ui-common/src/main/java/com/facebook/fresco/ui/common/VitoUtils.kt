/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

import java.util.concurrent.atomic.AtomicLong

object VitoUtils {

  private val idCounter = AtomicLong()

  /**
   * Create a unique Fresco Vito image ID.
   *
   * @return the new image ID to use
   */
  @JvmStatic fun generateIdentifier(): Long = idCounter.incrementAndGet()

  /** Create a string version for the given Vito ID. */
  @JvmStatic
  fun getStringId(id: Long): String =
      // Vito IDs and Drawee IDs overlap. We add a prefix to distinguish between them.
      "v$id"
}
