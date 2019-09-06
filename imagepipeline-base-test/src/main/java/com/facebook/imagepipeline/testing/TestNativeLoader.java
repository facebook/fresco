/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.testing;

import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.testloader.TestLoaderDelegateImpl;
import java.io.IOException;

/** Delegate to properly set and initialize NativeLoader for unit tests. */
public class TestNativeLoader {

  /** Initialize NativeLoader by setting TestLoaderDelegateImpl as delegate and calling init */
  public static void init() {
    NativeLoader.set(new TestLoaderDelegateImpl());
    try {
      NativeLoader.init(null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
