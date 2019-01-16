/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.platform;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import com.facebook.soloader.DoNotOptimize;

@DoNotOptimize
/*package*/ class PreverificationHelper {
  @TargetApi(Build.VERSION_CODES.O)
  @DoNotOptimize
  /*package*/ boolean shouldUseHardwareBitmapConfig(Bitmap.Config config) {
    return config == Bitmap.Config.HARDWARE;
  }
}
