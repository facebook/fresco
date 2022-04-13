/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.memory.config;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class MemorySpikeConfig {
  private static boolean sAvoidObjectsHashCode = false;

  public static void setAvoidObjectsHashCode(boolean avoidObjectsHashCode) {
    sAvoidObjectsHashCode = avoidObjectsHashCode;
  }

  public static boolean avoidObjectsHashCode() {
    return sAvoidObjectsHashCode;
  }
}
