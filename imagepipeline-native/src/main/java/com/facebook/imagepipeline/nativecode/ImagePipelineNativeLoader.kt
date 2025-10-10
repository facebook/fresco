/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import com.facebook.soloader.nativeloader.NativeLoader
import java.util.Collections

/**
 * Single place responsible for loading libimagepipeline.so and its dependencies.
 *
 * If your class has a native method whose implementation lives in libimagepipeline.so then call
 * [ImagePipelineNativeLoader#load] in its static initializer: ` public class ClassWithNativeMethod
 * { static { ImagePipelineNativeLoader.load(); }
 *
 * private static native void aNativeMethod(); } ` *
 */
object ImagePipelineNativeLoader {

  const val DSO_NAME: String = "imagepipeline"

  @JvmField var DEPENDENCIES: List<String>? = null

  init {
    val dependencies: List<String> = ArrayList()
    DEPENDENCIES = Collections.unmodifiableList(dependencies)
  }

  @JvmStatic
  fun load() {
    NativeLoader.loadLibrary("imagepipeline")
  }
}
