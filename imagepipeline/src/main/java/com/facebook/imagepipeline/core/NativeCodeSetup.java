/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import com.facebook.infer.annotation.Nullsafe;

/** Setter and getter for option about using native code. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class NativeCodeSetup {

  private static boolean sUseNativeCode = true;

  private NativeCodeSetup() {}

  /**
   * Setter for useNativeCode option
   *
   * @param useNativeCode true, if you want to use native code
   */
  public static void setUseNativeCode(boolean useNativeCode) {
    sUseNativeCode = useNativeCode;
  }

  /**
   * Getter for useNativeCode option
   *
   * @return true, if you going to use Native code
   */
  public static boolean getUseNativeCode() {
    return sUseNativeCode;
  }
}
