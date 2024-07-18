/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing

import com.facebook.soloader.NativeLoaderToSoLoaderDelegate
import com.facebook.soloader.SoLoader
import com.facebook.soloader.nativeloader.NativeLoader

/** Delegate to properly set and initialize NativeLoader for unit tests. */
object TestNativeLoader {
  /**
   * Initialize NativeLoader by setting NativeLoaderToSoLoaderDelegate as delegate and calling
   * setInTestMode for SoLoader
   */
  @JvmStatic
  fun init() {
    if (!NativeLoader.isInitialized()) {
      NativeLoader.init(NativeLoaderToSoLoaderDelegate())
    }
    SoLoader.setInTestMode()
  }
}
