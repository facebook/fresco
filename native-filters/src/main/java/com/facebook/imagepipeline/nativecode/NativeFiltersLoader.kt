/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import com.facebook.soloader.nativeloader.NativeLoader

/** Single place responsible for loading libnative-filters.so and its dependencies. */
object NativeFiltersLoader {
  @JvmStatic
  fun load() {
    NativeLoader.loadLibrary("native-filters")
  }
}
