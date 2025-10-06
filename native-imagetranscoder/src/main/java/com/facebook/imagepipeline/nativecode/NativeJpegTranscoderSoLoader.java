/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.soloader.nativeloader.NativeLoader;

/** Single place responsible for ensuring that native-imagetranscoder.so is loaded */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NativeJpegTranscoderSoLoader {
  private static boolean sInitialized;

  public static synchronized void ensure() {
    if (!sInitialized) {
      NativeLoader.loadLibrary("native-imagetranscoder");
      sInitialized = true;
    }
  }
}
