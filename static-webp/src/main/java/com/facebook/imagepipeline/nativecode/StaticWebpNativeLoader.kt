/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import android.os.Build
import com.facebook.soloader.nativeloader.NativeLoader

/** Single place responsible for ensuring that `static-webp.so` is loaded */
object StaticWebpNativeLoader {

  private var initialized = false

  @JvmStatic
  @Synchronized
  fun ensure() {
    if (!initialized) {
      // On Android 4.1.2 the loading of the static-webp native library can fail because
      // of the dependency with fb_jpegturbo. In this case we have to explicitely load that
      // library
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
        try {
          NativeLoader.loadLibrary("fb_jpegturbo")
        } catch (error: UnsatisfiedLinkError) {
          // Head in the sand
        }
      }
      NativeLoader.loadLibrary("static-webp")
      initialized = true
    }
  }
}
