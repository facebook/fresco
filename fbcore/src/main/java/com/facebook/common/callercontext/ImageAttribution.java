/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.callercontext;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface ImageAttribution {

  /**
   * In production, this often returns a null value. Keep that in mind when using this. See
   * T192655206 for example.
   */
  String getCallingClassName();

  @Nullable
  ContextChain getContextChain();
}
