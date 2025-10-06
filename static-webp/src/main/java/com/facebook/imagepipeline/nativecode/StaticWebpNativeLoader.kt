/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import com.facebook.soloader.nativeloader.NativeLoader

/** Single place responsible for ensuring that `static-webp.so` is loaded */
object StaticWebpNativeLoader {

  private var initialized = false

  @JvmStatic
  @Synchronized
  fun ensure() {
    if (!initialized) {
      NativeLoader.loadLibrary("static-webp")
      initialized = true
    }
  }
}
