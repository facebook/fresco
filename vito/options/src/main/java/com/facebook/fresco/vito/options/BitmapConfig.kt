/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import android.graphics.Bitmap
import android.os.Build

enum class BitmapConfig {
  ARGB_8888,
  RGBA_1010102,
  HARDWARE;

  fun toAndroidBitmapConfig(): Bitmap.Config {
    return when (this) {
      ARGB_8888 -> Bitmap.Config.ARGB_8888
      RGBA_1010102 -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          Bitmap.Config.RGBA_1010102
        } else {
          Bitmap.Config.ARGB_8888
        }
      }
      HARDWARE -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          Bitmap.Config.HARDWARE
        } else {
          Bitmap.Config.ARGB_8888
        }
      }
    }
  }
}
