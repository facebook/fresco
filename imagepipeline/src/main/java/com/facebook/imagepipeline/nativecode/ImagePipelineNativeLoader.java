/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.nativecode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.facebook.common.soloader.SoLoaderShim;

/**
 * Single place responsible for loading libimagepipeline.so and its dependencies.
 *
 * If your class has a native method whose implementation lives in libimagepipeline.so then call
 * {@link ImagePipelineNativeLoader#load} in its static initializer:
 * <code>
 *   public class ClassWithNativeMethod {
 *     static {
 *       ImagePipelineNativeLoader.load();
 *     }
 *
 *     private static native void aNativeMethod();
 *   }
 * </code>
 */
public class ImagePipelineNativeLoader {
  public static final String DSO_NAME = "imagepipeline";

  public static final List<String> DEPENDENCIES;
  static {
    List<String> dependencies = new ArrayList<String>();
    dependencies.add("webp");
    DEPENDENCIES = Collections.unmodifiableList(dependencies);
  }

  public static void load() {
    for (int i = 0; i < DEPENDENCIES.size(); ++i) {
      SoLoaderShim.loadLibrary(DEPENDENCIES.get(i));
    }
    SoLoaderShim.loadLibrary(DSO_NAME);
  }
}
