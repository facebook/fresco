/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.impl;

import com.facebook.callercontext.CallerContextVerifier;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class NoOpCallerContextVerifier implements CallerContextVerifier {

  @Override
  public void verifyCallerContext(@Nullable Object callerContext, boolean isPrefetch) {
    // No-op
  }
}
