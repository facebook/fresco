/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core

/** Setter and getter for option about using native code. */
object NativeCodeSetup {
  /**
   * Getter for useNativeCode option
   *
   * @return true, if you going to use Native code
   */
  /**
   * Setter for useNativeCode option
   *
   * @param useNativeCode true, if you want to use native code
   */
  @JvmStatic var useNativeCode: Boolean = true
}
