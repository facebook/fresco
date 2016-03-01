/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.memory;

import com.facebook.common.soloader.SoLoaderShim;

import org.junit.BeforeClass;

/**
 * Base class for tests that indirectly use NativeMemoryChunk.
 *
 * <p>These need to stub out the native code called from NMC's static initializer.
 */
public class TestUsingNativeMemoryChunk {

  @BeforeClass
  public static void allowNativeStaticInitializers() {
    SoLoaderShim.setHandler(
        new SoLoaderShim.Handler() {
          @Override
          public void loadLibrary(String libraryName) {
            // ignore it
          }
        });
  }
}
