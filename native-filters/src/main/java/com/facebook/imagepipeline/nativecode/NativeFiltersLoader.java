/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import com.facebook.soloader.SoLoader;

/** Single place responsible for loading libnative-filters.so and its dependencies. */
public class NativeFiltersLoader {

  public static void load() {
    SoLoader.loadLibrary("native-filters");
  }
}
