/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.memory.helper;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class HashCode {
  public static int extend(int current, final @Nullable Object obj) {
    return 31 * current + (obj == null ? 0 : obj.hashCode());
  }
}
