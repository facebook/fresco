/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import android.os.Build;
import com.facebook.soloader.SoLoader;

/**
 * Single place responsible for ensuring that `static-webp.so` is loaded
 */
public class StaticWebpNativeLoader {

  private static boolean sInitialized;

  public static synchronized void ensure() {
    if (!sInitialized) {
      // On Android 4.1.2 the loading of the static-webp native library can fail because
      // of the dependency with fb_jpegturbo. In this case we have to explicitely load that
      // library
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
        try {
          SoLoader.loadLibrary("fb_jpegturbo");
        } catch (UnsatisfiedLinkError error) {
          // Head in the sand
        }
      }
      SoLoader.loadLibrary("static-webp");
      sInitialized = true;
    }
  }
}
