/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider.impl;

import com.facebook.callercontext.CallerContextVerifier;
import javax.annotation.Nullable;

public class NoOpCallerContextVerifier implements CallerContextVerifier {

  @Override
  public void verifyCallerContext(@Nullable Object callerContext, boolean isPrefetch) {
    // No-op
  }
}
