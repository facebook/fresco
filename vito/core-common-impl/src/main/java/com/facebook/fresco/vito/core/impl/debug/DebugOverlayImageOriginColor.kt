/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Color
import androidx.annotation.ColorInt

object DebugOverlayImageOriginColor {

  private val imageOriginColorMap: Map<String, Int> =
      hashMapOf(
          "unknown" to Color.GRAY,
          "network" to Color.RED,
          "disk" to Color.YELLOW,
          "memory_encoded" to Color.YELLOW,
          "memory_bitmap" to Color.GREEN,
          "local" to Color.GREEN)

  @JvmStatic
  @ColorInt
  fun getImageOriginColor(imageOrigin: String): Int =
      imageOriginColorMap[imageOrigin] ?: Color.WHITE
}
