/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import androidx.annotation.Nullable;

/** Verifies CallerContext */
public interface CallerContextVerifier {

  void verifyCallerContext(@Nullable Object callerContext);
}
