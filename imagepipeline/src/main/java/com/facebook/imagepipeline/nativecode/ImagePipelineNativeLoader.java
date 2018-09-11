/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import com.facebook.soloader.SoLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    DEPENDENCIES = Collections.unmodifiableList(dependencies);
  }

  public static void load() {
    SoLoader.loadLibrary("imagepipeline");
  }
}
