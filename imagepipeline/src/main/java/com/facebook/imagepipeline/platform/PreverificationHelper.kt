/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import com.facebook.soloader.DoNotOptimize

@DoNotOptimize
internal class PreverificationHelper {
  @TargetApi(Build.VERSION_CODES.O)
  @DoNotOptimize
  fun /*package*/ shouldUseHardwareBitmapConfig(config: Bitmap.Config?): Boolean =
      config == Bitmap.Config.HARDWARE
}
