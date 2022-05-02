/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import com.facebook.imagepipeline.testing.TestNativeLoader;
import org.junit.BeforeClass;

/**
 * Base class for tests that indirectly use NativeMemoryChunk.
 *
 * <p>These need to stub out the native code called from NMC's static initializer.
 */
public class TestUsingNativeMemoryChunk {

  @BeforeClass
  public static void allowNativeStaticInitializers() {
    TestNativeLoader.init();
  }
}
