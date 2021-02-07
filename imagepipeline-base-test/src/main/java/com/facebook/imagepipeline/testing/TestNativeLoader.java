/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.infer.annotation.Nullsafe;
import com.facebook.soloader.NativeLoaderToSoLoaderDelegate;
import com.facebook.soloader.SoLoader;
import com.facebook.soloader.nativeloader.NativeLoader;

/** Delegate to properly set and initialize NativeLoader for unit tests. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class TestNativeLoader {

  /**
   * Initialize NativeLoader by setting NativeLoaderToSoLoaderDelegate as delegate and calling
   * setInTestMode for SoLoader
   */
  public static void init() {
    if (!NativeLoader.isInitialized()) {
      NativeLoader.init(new NativeLoaderToSoLoaderDelegate());
    }
    SoLoader.setInTestMode();
  }
}
