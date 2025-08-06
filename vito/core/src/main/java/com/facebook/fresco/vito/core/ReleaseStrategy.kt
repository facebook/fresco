/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import com.facebook.fresco.vito.core.ReleaseStrategy.DELAYED
import com.facebook.fresco.vito.core.ReleaseStrategy.IMMEDIATE
import com.facebook.fresco.vito.core.ReleaseStrategy.NEXT_FRAME

/** Defines how an image is released. */
enum class ReleaseStrategy {
  IMMEDIATE,
  DELAYED,
  NEXT_FRAME;

  companion object {
    @JvmStatic
    fun parse(value: Long): ReleaseStrategy =
        when (value) {
          2L -> IMMEDIATE
          1L -> DELAYED
          else -> NEXT_FRAME
        }

    fun FrescoController2.release(
        drawable: FrescoDrawableInterface,
        releaseStrategy: ReleaseStrategy,
    ) {
      when (releaseStrategy) {
        IMMEDIATE -> releaseImmediately(drawable)
        DELAYED -> releaseDelayed(drawable)
        NEXT_FRAME -> release(drawable)
      }
    }
  }
}
