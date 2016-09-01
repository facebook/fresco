/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import android.os.Build;

import com.facebook.common.soloader.SoLoaderShim;

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
          SoLoaderShim.loadLibrary("fb_jpegturbo");
        } catch (UnsatisfiedLinkError error) {
          // Head in the sand
        }
      }
      SoLoaderShim.loadLibrary("static-webp");
      sInitialized = true;
    }
  }
}
